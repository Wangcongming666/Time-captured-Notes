package cn.shiguang.notes.repository.support;

import cn.shiguang.notes.entity.BaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.*;
import java.util.*;
import org.springframework.beans.BeanWrapperImpl;

/** Whitelisted entity properties; all persistence is performed by the real typed Mapper. */
public final class EntityWrites {
  private EntityWrites() {}

  public static <T extends BaseEntity> long insert(
      BaseMapper<T> mapper, Class<T> type, Map<String, Object> values) {
    try {
      T entity = type.getDeclaredConstructor().newInstance();
      var bean = new BeanWrapperImpl(entity);
      for (var entry : values.entrySet()) {
        String property = camel(entry.getKey());
        if (!bean.isWritableProperty(property))
          throw new IllegalArgumentException("Unknown persistent property: " + entry.getKey());
        Object value = entry.getValue();
        if (value instanceof String text && bean.getPropertyType(property) == LocalDateTime.class) {
          value =
              text.endsWith("Z")
                  ? LocalDateTime.ofInstant(Instant.parse(text), ZoneOffset.UTC)
                  : LocalDateTime.parse(text);
        }
        bean.setPropertyValue(property, value);
      }
      if (mapper.insert(entity) != 1 || entity.getId() == null)
        throw new IllegalStateException("Entity insert did not generate an id");
      return entity.getId();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Invalid entity type", e);
    }
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
