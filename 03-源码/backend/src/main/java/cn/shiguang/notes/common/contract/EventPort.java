package cn.shiguang.notes.common.contract;

import java.util.*;

/** Durable event append contract; requires an existing business transaction. */
public interface EventPort {
  void append(
      String type,
      long actor,
      String aggregateType,
      long aggregateId,
      int revision,
      Map<String, Object> payload,
      String... consumers);
}
