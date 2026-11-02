package cn.shiguang.notes.schedule;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.support.MapperRows;
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
  private final OutboxMapper outboxMapper;
  private final TransactionTemplate tx;
  private final RabbitTemplate rabbit;

  public OutboxDispatcher(
      OutboxMapper outboxMapper, TransactionTemplate tx, RabbitTemplate rabbit) {
    this.outboxMapper = outboxMapper;
    this.tx = tx;
    this.rabbit = rabbit;
  }

  @Scheduled(fixedDelay = 1000, initialDelay = 3000)
  public void dispatch() {
    List<Map<String, Object>> events =
        tx.execute(
            s -> {
              outboxMapper.outboxDispatcherDispatch();
              var rows = MapperRows.entities(outboxMapper.outboxDispatcherDispatch2());
              for (var r : rows) {
                String token = UUID.randomUUID().toString();
                outboxMapper.outboxDispatcherDispatch3(token, Data.id(r, "id"));
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
        outboxMapper.outboxDispatcherDispatch4(id, token);
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        outboxMapper.outboxDispatcherDispatch5(ex.getClass().getSimpleName(), id, token);
        LOG.warn("Outbox publish pending id={} type={}", id, ex.getClass().getSimpleName());
      }
    }
  }
}
