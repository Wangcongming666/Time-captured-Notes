package cn.shiguang.notes.schedule;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.repository.support.Database;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Short database leases, broker calls outside transactions, terminal-state CAS. */
@Component
public class OutboxDispatcher {
  private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatcher.class);
  private final Database db;
  private final TransactionTemplate tx;
  private final RabbitTemplate rabbit;

  public OutboxDispatcher(Database db, TransactionTemplate tx, RabbitTemplate rabbit) {
    this.db = db;
    this.tx = tx;
    this.rabbit = rabbit;
  }

  @Scheduled(fixedDelay = 1000, initialDelay = 3000)
  public void dispatch() {
    List<Map<String, Object>> events =
        tx.execute(
            s -> {
              db.update(
                  "UPDATE t_outbox SET status='PENDING',lease_token=NULL WHERE (status='LEASED' AND"
                      + " lease_until<UTC_TIMESTAMP(3)) OR (status='SENT' AND"
                      + " updated_at<DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 60 SECOND))");
              var rows =
                  db.query(
                      "SELECT * FROM t_outbox WHERE status='PENDING' AND"
                          + " available_at<=UTC_TIMESTAMP(3) ORDER BY id LIMIT 50 FOR UPDATE SKIP"
                          + " LOCKED");
              for (var r : rows) {
                String token = UUID.randomUUID().toString();
                db.update(
                    "UPDATE t_outbox SET"
                        + " status='LEASED',lease_token=?,lease_until=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL"
                        + " 30 SECOND),publish_attempts=publish_attempts+1 WHERE id=? AND"
                        + " status='PENDING'",
                    token,
                    Data.id(r, "id"));
                r.put("leaseToken", token);
              }
              return rows;
            });
    if (events == null) return;
    for (var event : events) {
      long id = Data.id(event, "id");
      String token = Data.text(event, "leaseToken");
      try {
        var props = new MessageProperties();
        props.setContentType("application/json");
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setHeader("outboxId", Long.toString(id));
        props.setHeader("leaseToken", token);
        var correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbit.send(
            "sg.events",
            Data.text(event, "routingKey"),
            new Message(Data.text(event, "payload").getBytes(StandardCharsets.UTF_8), props),
            correlation);
        var confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
        if (!confirm.isAck() || correlation.getReturned() != null)
          throw new IllegalStateException("Broker did not accept route");
        db.update(
            "UPDATE t_outbox SET status='SENT' WHERE id=? AND status='LEASED' AND lease_token=?",
            id,
            token);
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        db.update(
            "UPDATE t_outbox SET"
                + " status='PENDING',lease_token=NULL,available_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL"
                + " 5 SECOND),last_error=? WHERE id=? AND status='LEASED' AND lease_token=?",
            ex.getClass().getSimpleName(),
            id,
            token);
        LOG.warn("Outbox publish pending id={} type={}", id, ex.getClass().getSimpleName());
      }
    }
  }
}
