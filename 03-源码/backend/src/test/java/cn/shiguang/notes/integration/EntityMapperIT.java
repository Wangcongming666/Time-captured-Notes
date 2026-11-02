package cn.shiguang.notes.integration;

import static org.junit.jupiter.api.Assertions.*;

import cn.shiguang.notes.architecture.EntitySchemaContractTest;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Only MySQL + Flyway + the real MP mapper proxies; no HTTP, Redis or Rabbit context. */
@Testcontainers
@SuppressWarnings({"rawtypes", "unchecked"})
class EntityMapperIT {
  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4.13")
          .withDatabaseName("entity_contract")
          .withUsername("contract")
          .withPassword("contract")
          .withCommand("--default-time-zone=+00:00");

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final LocalDateTime INSTANT = LocalDateTime.of(2026, 1, 2, 3, 4, 5, 123_000_000);
  private static DriverManagerDataSource dataSource;
  private static SqlSessionFactory sessions;
  private static Map<String, Class<?>> entities;

  @BeforeAll
  static void migrateAndRegisterAllEntityMappers() throws Exception {
    String url = MYSQL.getJdbcUrl();
    url +=
        (url.contains("?") ? "&" : "?")
            + "connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&tinyInt1isBit=false";
    dataSource = new DriverManagerDataSource(url, MYSQL.getUsername(), MYSQL.getPassword());
    Flyway.configure().dataSource(dataSource).load().migrate();
    entities = EntitySchemaContractTest.entities();
    var configuration = new MybatisConfiguration();
    configuration.setMapUnderscoreToCamelCase(true);
    for (Class<?> entity : entities.values())
      configuration.addMapper(EntitySchemaContractTest.mapper(entity));
    var factory = new MybatisSqlSessionFactoryBean();
    factory.setDataSource(dataSource);
    factory.setConfiguration(configuration);
    sessions = factory.getObject();
    assertNotNull(sessions);
  }

  @Test
  void everyTableReallyInsertsMapsDefaultsAndRoundTripsAllItsColumns() throws Exception {
    var schema = EntitySchemaContractTest.schema();
    assertEquals(30, entities.size());
    assertEquals(schema.keySet(), entities.keySet());
    var ids = new HashMap<String, Long>();
    try (var connection = dataSource.getConnection();
        var session = sessions.openSession(true)) {
      try (var query = connection.createStatement();
          var result = query.executeQuery("SELECT @@session.time_zone")) {
        assertTrue(result.next());
        assertEquals("+00:00", result.getString(1), "DATETIME convention must be UTC");
      }
      for (var table : schema.entrySet()) {
        String name = table.getKey();
        Class<?> entityType = entities.get(name);
        BaseMapper mapper =
            (BaseMapper) session.getMapper(EntitySchemaContractTest.mapper(entityType));
        var fields = EntitySchemaContractTest.fields(entityType);
        var columns = metadata(connection, name);
        assertEquals(
            table.getValue().keySet(), columns.keySet(), name + " migrated columns, including V3");
        for (var column : table.getValue().values()) {
          assertEquals(
              column.sqlType().toUpperCase(Locale.ROOT),
              columns.get(column.name()).type(),
              name + "." + column.name());
          assertEquals(column.javaType(), fields.get(column.name()).getType());
        }
        var references = foreignKeys(connection, name);
        Object entity = entityType.getConstructor().newInstance();
        for (var column : columns.values()) {
          if (!column.autoIncrement() && !column.nullable() && column.defaultValue() == null)
            write(entity, fields.get(column.name()), sample(column, references, ids, null));
        }
        long before = mapper.selectCount(null);
        assertEquals(1, mapper.insert(entity), name + " insert");
        Long id = (Long) read(entity, fields.get("id"));
        assertNotNull(id, name + " generated id must be written back");
        assertTrue(id > 0);
        ids.put(name, id);
        assertEquals(before + 1, mapper.selectCount(null), name + " selectCount");
        assertDatabaseRowMatches(connection, name, id, mapper.selectById(id), fields, columns);
        // Populate even nullable/defaulted fields, so a silently missing result mapping cannot
        // pass.
        for (var column : columns.values()) {
          if (!column.name().equals("id"))
            write(entity, fields.get(column.name()), sample(column, references, ids, id));
        }
        assertEquals(1, mapper.updateById(entity), name + " update all fields");
        Object loaded = mapper.selectById(id);
        assertDatabaseRowMatches(connection, name, id, loaded, fields, columns);
        for (var column : columns.values()) {
          Object expected = column.name().equals("id") ? id : sample(column, references, ids, id);
          assertEquivalent(
              expected,
              read(loaded, fields.get(column.name())),
              column.type(),
              name + "." + column.name());
        }
        // NOT_NULL patch must preserve old values; explicit wrapper SET must still clear a column.
        var nullable = columns.values().stream().filter(Column::nullable).findFirst();
        if (nullable.isPresent()) {
          Column column = nullable.get();
          Object patch = entityType.getConstructor().newInstance();
          write(patch, fields.get("id"), id);
          // Include one real assignment so the patch always produces syntactically valid SQL.
          write(patch, fields.get("created_at"), INSTANT);
          assertEquals(1, mapper.updateById(patch));
          assertEquivalent(
              read(loaded, fields.get(column.name())),
              read(mapper.selectById(id), fields.get(column.name())),
              column.type(),
              name + " null patch preserves value");
          var clear = new UpdateWrapper<>().eq("id", id).set(column.name(), null);
          // Clearing VIDEO metadata must atomically switch back to IMAGE to obey ck_video_media.
          if (name.equals("t_media")) clear.set("media_type", "IMAGE");
          assertEquals(1, mapper.update(null, clear));
          assertNull(
              read(mapper.selectById(id), fields.get(column.name())), name + " explicit SQL NULL");
        }
        // CHECK(follower_id <> followee_id) requires a second actual user, never disabled
        // constraints.
        if (name.equals("t_user")) {
          Object other = entityType.getConstructor().newInstance();
          write(other, fields.get("username"), "second_user");
          write(other, fields.get("password_hash"), "hash");
          write(other, fields.get("nickname"), "second");
          assertEquals(1, mapper.insert(other));
          ids.put("second_user", (Long) read(other, fields.get("id")));
        }
      }
    }
  }

  private record Column(
      String name, String type, boolean nullable, String defaultValue, boolean autoIncrement) {}

  private static Map<String, Column> metadata(Connection connection, String table)
      throws Exception {
    var columns = new LinkedHashMap<String, Column>();
    try (var query =
        connection.prepareStatement(
            "SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA FROM"
                + " information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? ORDER"
                + " BY ORDINAL_POSITION")) {
      query.setString(1, table);
      try (var result = query.executeQuery()) {
        while (result.next()) {
          String name = result.getString(1);
          columns.put(
              name,
              new Column(
                  name,
                  result.getString(2).toUpperCase(Locale.ROOT),
                  result.getString(3).equals("YES"),
                  result.getString(4),
                  result.getString(5).contains("auto_increment")));
        }
      }
    }
    return columns;
  }

  private static Map<String, String> foreignKeys(Connection connection, String table)
      throws Exception {
    Map<String, String> result = new HashMap<>();
    try (var keys =
        connection.getMetaData().getImportedKeys(connection.getCatalog(), null, table)) {
      while (keys.next())
        result.put(keys.getString("FKCOLUMN_NAME"), keys.getString("PKTABLE_NAME"));
    }
    return result;
  }

  private static Object sample(
      Column column, Map<String, String> references, Map<String, Long> ids, Long ownId) {
    if (column.name().equals("followee_id")) return Objects.requireNonNull(ids.get("second_user"));
    if (references.containsKey(column.name())) {
      Long id = ids.get(references.get(column.name()));
      return id != null ? id : ownId; // comment root/reply refer to the inserted row itself
    }
    return switch (column.type()) {
      case "BIGINT" -> column.name().equals("size_bytes") ? 5_000_000_123L : 123L;
      case "INT" -> column.name().equals("version") ? 7 : 2;
      case "TINYINT" -> 1;
      case "DECIMAL" -> new BigDecimal("1234567890123456.1234");
      case "DATE" -> LocalDate.of(2026, 1, 2);
      case "DATETIME" -> INSTANT;
      case "JSON" -> "{\"title\":\"中文记录\",\"nested\":{\"ok\":true},\"items\":[1,null,3]}";
      default ->
          switch (column.name()) {
            case "media_type" -> "VIDEO";
            case "purpose" -> "NOTE";
            case "mime" -> "video/mp4";
            default -> "fixture";
          };
    };
  }

  private static void assertDatabaseRowMatches(
      Connection connection,
      String table,
      Long id,
      Object entity,
      Map<String, Field> fields,
      Map<String, Column> columns)
      throws Exception {
    assertNotNull(entity, table + " selectById");
    try (var query = connection.prepareStatement("SELECT * FROM `" + table + "` WHERE id=?")) {
      query.setLong(1, id);
      try (var row = query.executeQuery()) {
        assertTrue(row.next());
        for (var column : columns.values()) {
          Object expected =
              switch (column.type()) {
                case "DATETIME" -> row.getObject(column.name(), LocalDateTime.class);
                case "DATE" -> row.getObject(column.name(), LocalDate.class);
                case "BIGINT" -> row.getObject(column.name(), Long.class);
                case "INT", "TINYINT" -> row.getObject(column.name(), Integer.class);
                case "JSON" -> row.getString(column.name());
                default -> row.getObject(column.name());
              };
          assertEquivalent(
              expected,
              read(entity, fields.get(column.name())),
              column.type(),
              table + "." + column.name());
          if (!column.nullable())
            assertNotNull(expected, table + "." + column.name() + " DB default/required value");
        }
      }
    }
  }

  private static void assertEquivalent(Object expected, Object actual, String type, String label)
      throws Exception {
    if (expected != null && actual != null && type.equals("JSON"))
      assertEquals(JSON.readTree(expected.toString()), JSON.readTree(actual.toString()), label);
    else if (expected instanceof BigDecimal decimal && actual instanceof BigDecimal other)
      assertEquals(0, decimal.compareTo(other), label);
    else assertEquals(expected, actual, label);
  }

  private static Object read(Object entity, Field field) throws Exception {
    for (var property : Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors())
      if (property.getName().equals(field.getName()))
        return property.getReadMethod().invoke(entity);
    throw new AssertionError("Missing getter: " + field);
  }

  private static void write(Object entity, Field field, Object value) throws Exception {
    for (var property : Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors()) {
      if (property.getName().equals(field.getName())) {
        property.getWriteMethod().invoke(entity, value);
        return;
      }
    }
    throw new AssertionError("Missing setter: " + field);
  }
}
