package cn.shiguang.notes.platform.persistence;

import cn.shiguang.notes.shared.error.BizException;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Prepared SQL utility used only by infrastructure/read models, never controllers. */
@Repository
public class Database {
  private final JdbcTemplate jdbc;
  private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

  public Database(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> query(String sql, Object... args) {
    return jdbc.query(
        sql,
        (rs, index) -> {
          var row = new LinkedHashMap<String, Object>();
          var meta = rs.getMetaData();
          for (int i = 1; i <= meta.getColumnCount(); i++) {
            String raw = meta.getColumnLabel(i);
            String key = camel(raw);
            Object v = rs.getObject(i);
            if (v instanceof Number
                && (raw.equals("id") || raw.endsWith("_id") || key.endsWith("Id")))
              v = v.toString();
            if (v instanceof Timestamp t) v = t.toInstant().toString();
            if (v instanceof LocalDateTime t) v = t.toInstant(ZoneOffset.UTC).toString();
            if (v instanceof java.sql.Date t) v = t.toLocalDate().toString();
            row.put(key, v);
          }
          return row;
        },
        args);
  }

  public Map<String, Object> optional(String sql, Object... args) {
    var list = query(sql, args);
    return list.isEmpty() ? null : list.getFirst();
  }

  public Map<String, Object> one(String sql, Object... args) {
    var row = optional(sql, args);
    if (row == null) throw BizException.missing();
    return row;
  }

  public long count(String sql, Object... args) {
    Long n = jdbc.queryForObject(sql, Long.class, args);
    return n == null ? 0 : n;
  }

  public int update(String sql, Object... args) {
    return jdbc.update(sql, args);
  }

  public long insert(String table, Map<String, Object> values) {
    if (!NAME.matcher(table).matches()
        || values.keySet().stream().anyMatch(k -> !NAME.matcher(k).matches()))
      throw new IllegalArgumentException("Invalid internal SQL identifier");
    var keys = new ArrayList<>(values.keySet());
    var sql =
        "INSERT INTO `"
            + table
            + "` ("
            + String.join(",", keys.stream().map(k -> "`" + k + "`").toList())
            + ") VALUES ("
            + String.join(",", Collections.nCopies(keys.size(), "?"))
            + ")";
    var holder = new GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < keys.size(); i++) ps.setObject(i + 1, values.get(keys.get(i)));
          return ps;
        },
        holder);
    return Objects.requireNonNull(holder.getKey()).longValue();
  }

  private static String camel(String raw) {
    StringBuilder b = new StringBuilder();
    boolean up = false;
    for (char c : raw.toCharArray()) {
      if (c == '_') {
        up = true;
      } else {
        b.append(up ? Character.toUpperCase(c) : c);
        up = false;
      }
    }
    return b.toString();
  }
}
