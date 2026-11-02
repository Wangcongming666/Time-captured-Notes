package cn.shiguang.notes.messaging;

import cn.shiguang.notes.common.contract.CachePort;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.support.MapperRows;
import cn.shiguang.notes.service.NotificationService;
import cn.shiguang.notes.service.StatisticsService;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** The inbox, business side effect and DONE marker share exactly one local transaction. */
@Component
public class InboxTransactions {
  private final InboxMapper inboxMapper;
  private final OutboxMapper outboxMapper;
  private final TransactionTemplate tx;
  private final NotificationService notifications;
  private final StatisticsService statistics;
  private final CachePort cache;

  public InboxTransactions(
      InboxMapper inboxMapper,
      OutboxMapper outboxMapper,
      TransactionTemplate tx,
      NotificationService notifications,
      StatisticsService statistics,
      CachePort cache) {
    this.inboxMapper = inboxMapper;
    this.outboxMapper = outboxMapper;
    this.tx = tx;
    this.notifications = notifications;
    this.statistics = statistics;
    this.cache = cache;
  }

  public String consume(long id, String token, String consumer, Map<String, Object> event) {
    return tx.execute(
        status -> {
          var row = MapperRows.entity(outboxMapper.inboxTransactionsConsume(id));
          if (row == null) return "STALE";
          if (!consumer.equals(row.get("consumer"))
              || !Data.text(event, "eventId").equals(Data.text(row, "eventId")))
            throw new IllegalArgumentException("Message route does not match durable event");
          if ("DONE".equals(row.get("status"))) return "DONE";
          if (inboxMapper.inboxTransactionsConsume(consumer, Data.text(row, "eventId")) > 0) {
            outboxMapper.inboxTransactionsConsume2(id);
            return "DONE";
          }
          if ("DEAD".equals(row.get("status"))) return "DEAD";
          if (!Set.of("LEASED", "SENT").contains(Data.text(row, "status"))
              || !Objects.equals(token, row.get("leaseToken"))) return "STALE";
          inboxMapper.inboxTransactionsConsume2(consumer, Data.text(row, "eventId"));
          switch (consumer) {
            case "notification" -> notifications.receive(event);
            case "statistics" -> statistics.receive(event);
            case "cache" -> cache.removePrefix("sg:feed:");
            default -> throw new IllegalArgumentException("Unknown consumer");
          }
          outboxMapper.inboxTransactionsConsume3(id);
          return "DONE";
        });
  }

  public String failed(long id, String token, Exception error) {
    return tx.execute(
        status -> {
          var row = MapperRows.entity(outboxMapper.inboxTransactionsFailed(id));
          if (row == null || "DONE".equals(row.get("status"))) return "DONE";
          if (!Objects.equals(token, row.get("leaseToken"))) return "STALE";
          int attempts = Data.number(row, "attempts") + 1;
          String next = attempts >= 5 ? "DEAD" : "PENDING";
          int delay = new int[] {5, 30, 120, 300, 300}[Math.min(attempts - 1, 4)];
          outboxMapper.inboxTransactionsFailed2(
              next, attempts, delay, error.getClass().getSimpleName(), id, token);
          return next;
        });
  }
}
