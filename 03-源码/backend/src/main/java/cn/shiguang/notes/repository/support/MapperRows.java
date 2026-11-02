package cn.shiguang.notes.repository.support;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.entity.BaseEntity;
import java.beans.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/** Internal projections only. HTTP responses are explicit VO types. */
public final class MapperRows {
  private MapperRows() {}

  public static List<Map<String, Object>> list(List<Map<String, Object>> rows) {
    return rows.stream()
        .map(MapperRows::normalize)
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }

  public static Map<String, Object> one(List<Map<String, Object>> rows) {
    var row = optional(rows);
    if (row == null) throw BizException.missing();
    return row;
  }

  public static Map<String, Object> optional(List<Map<String, Object>> rows) {
    return rows.isEmpty() ? null : normalize(rows.getFirst());
  }

  public static Map<String, Object> entity(BaseEntity bean) {
    if (bean == null) return null;
    var out = new LinkedHashMap<String, Object>();
    try {
      for (var property :
          Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors()) {
        if (property.getReadMethod() != null)
          out.put(
              property.getName(),
              normalizeValue(property.getName(), property.getReadMethod().invoke(bean)));
      }
    } catch (ReflectiveOperationException | IntrospectionException e) {
      throw new IllegalStateException("Invalid persistent bean", e);
    }
    return out;
  }

  public static Map<String, Object> requiredEntity(BaseEntity bean) {
    var row = entity(bean);
    if (row == null) throw BizException.missing();
    return row;
  }

  public static List<Map<String, Object>> entities(List<? extends BaseEntity> values) {
    return values.stream()
        .map(MapperRows::entity)
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }

  private static Map<String, Object> normalize(Map<String, Object> row) {
    var out = new LinkedHashMap<String, Object>();
    row.forEach(
        (key, value) -> {
          String name = camel(key);
          out.put(name, normalizeValue(name, value));
        });
    return out;
  }

  private static Object normalizeValue(String key, Object value) {
    if (value instanceof Number && (key.equals("id") || key.endsWith("Id")))
      return value.toString();
    if (value instanceof Timestamp time) return time.toInstant().toString();
    if (value instanceof LocalDateTime time) return time.toInstant(ZoneOffset.UTC).toString();
    if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
    if (value instanceof LocalDate date) return date.toString();
    return value;
  }

  private static String camel(String raw) {
    var out = new StringBuilder();
    boolean upper = false;
    for (char c : raw.toCharArray()) {
      if (c == '_') upper = true;
      else {
        out.append(upper ? Character.toUpperCase(c) : c);
        upper = false;
      }
    }
    return out.toString();
  }
}
