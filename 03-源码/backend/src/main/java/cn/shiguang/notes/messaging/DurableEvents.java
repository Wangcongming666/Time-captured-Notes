package cn.shiguang.notes.messaging;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Appends a consumer-specific recoverable event in the existing transaction. */
@Component
public class DurableEvents implements EventPort {
  private final OutboxMapper outboxMapper;
  private final ObjectMapper json;

  public DurableEvents(OutboxMapper outboxMapper, ObjectMapper json) {
    this.outboxMapper = outboxMapper;
    this.json = json;
  }

  public void append(
      String type,
      long actor,
      String aggregate,
      long id,
      int revision,
      Map<String, Object> payload,
      String... consumers) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Outbox requires business transaction");
    if (outboxMapper.durableEventsAppend() > 100000)
      throw new BizException(503, "EVENT_BACKLOG", "消息积压，请稍后重试");
    String event = UUID.randomUUID().toString();
    var value =
        Data.map(
            "eventId",
            event,
            "eventType",
            type,
            "schemaVersion",
            1,
            "occurredAt",
            Instant.now().toString(),
            "actorId",
            Long.toString(actor),
            "aggregateType",
            aggregate,
            "aggregateId",
            Long.toString(id),
            "aggregateRevision",
            revision,
            "traceId",
            Objects.toString(MDC.get("traceId"), "scheduled"),
            "payload",
            payload);
    try {
      String encoded = json.writeValueAsString(value);
      for (String consumer : Set.of(consumers))
        outboxMapper.durableEventsAppend2(event, consumer, type + "." + consumer, encoded);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid event payload", e);
    }
  }
}
