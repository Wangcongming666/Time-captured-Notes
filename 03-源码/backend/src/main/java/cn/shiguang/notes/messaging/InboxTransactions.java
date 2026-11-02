package cn.shiguang.notes.messaging;

import cn.shiguang.notes.common.contract.CachePort;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.repository.support.Database;
import cn.shiguang.notes.service.NotificationService;
import cn.shiguang.notes.service.StatisticsService;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** The inbox, business side effect and DONE marker share exactly one local transaction. */
@Component
public class InboxTransactions {
  private final Database db;
  private final TransactionTemplate tx;
  private final NotificationService notifications;
  private final StatisticsService statistics;
  private final CachePort cache;

  public InboxTransactions(
      Database db,
      TransactionTemplate tx,
      NotificationService notifications,
      StatisticsService statistics,
      CachePort cache) {
    this.db = db;
    this.tx = tx;
    this.notifications = notifications;
    this.statistics = statistics;
    this.cache = cache;
  }

  public String consume(long id, String token, String consumer, Map<String, Object> event) {
    return tx.execute(
        status -> {
          var row = db.optional("SELECT * FROM t_outbox WHERE id=? FOR UPDATE", id);
          if (row == null) return "STALE";
          if (!consumer.equals(row.get("consumer"))
              || !Data.text(event, "eventId").equals(row.get("eventId")))
            throw new IllegalArgumentException("Message route does not match durable event");
          if ("DONE".equals(row.get("status"))) return "DONE";
          if (db.count(
                  "SELECT COUNT(*) FROM t_inbox WHERE consumer=? AND event_id=?",
                  consumer,
                  row.get("eventId"))
              > 0) {
            db.update("UPDATE t_outbox SET status='DONE',lease_token=NULL WHERE id=?", id);
            return "DONE";
          }
          if ("DEAD".equals(row.get("status"))) return "DEAD";
          if (!Set.of("LEASED", "SENT").contains(Data.text(row, "status"))
              || !Objects.equals(token, row.get("leaseToken"))) return "STALE";
          db.update(
              "INSERT INTO t_inbox(consumer,event_id,processed_at) VALUES(?,?,UTC_TIMESTAMP(3))",
              consumer,
              row.get("eventId"));
          switch (consumer) {
            case "notification" -> notifications.receive(event);
            case "statistics" -> statistics.receive(event);
            case "cache" -> cache.removePrefix("sg:feed:");
            default -> throw new IllegalArgumentException("Unknown consumer");
          }
          db.update(
              "UPDATE t_outbox SET status='DONE',lease_token=NULL,last_error=NULL WHERE id=?", id);
          return "DONE";
        });
  }

  public String failed(long id, String token, Exception error) {
    return tx.execute(
        status -> {
          var row = db.optional("SELECT * FROM t_outbox WHERE id=? FOR UPDATE", id);
          if (row == null || "DONE".equals(row.get("status"))) return "DONE";
          if (!Objects.equals(token, row.get("leaseToken"))) return "STALE";
          int attempts = Data.number(row, "attempts") + 1;
          String next = attempts >= 5 ? "DEAD" : "PENDING";
          int delay = new int[] {5, 30, 120, 300, 300}[Math.min(attempts - 1, 4)];
          db.update(
              "UPDATE t_outbox SET"
                  + " status=?,attempts=?,lease_token=NULL,available_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL"
                  + " ? SECOND),last_error=? WHERE id=? AND lease_token=? AND status IN"
                  + " ('LEASED','SENT')",
              next,
              attempts,
              delay,
              error.getClass().getSimpleName(),
              id,
              token);
          return next;
        });
  }
}
