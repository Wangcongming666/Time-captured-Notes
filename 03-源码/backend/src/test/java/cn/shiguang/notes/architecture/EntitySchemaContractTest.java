package cn.shiguang.notes.architecture;

import static org.junit.jupiter.api.Assertions.*;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.beans.Introspector;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;

/** The migration is the source of truth; no second handwritten list of thirty entities. */
public class EntitySchemaContractTest {
  public record Column(String name, String sqlType) {
    public Class<?> javaType() {
      return switch (sqlType.toUpperCase(Locale.ROOT)) {
        case "BIGINT" -> Long.class;
        case "INT", "TINYINT" -> Integer.class;
        case "DATETIME" -> LocalDateTime.class;
        case "DATE" -> LocalDate.class;
        case "DECIMAL" -> BigDecimal.class;
        case "VARCHAR", "CHAR", "TEXT", "JSON" -> String.class;
        default -> throw new AssertionError("Unspecified SQL type: " + sqlType);
      };
    }
  }

  public static Map<String, LinkedHashMap<String, Column>> schema() throws Exception {
    Map<String, LinkedHashMap<String, Column>> tables = new LinkedHashMap<>();
    var create =
        Pattern.compile("CREATE TABLE `([^`]+)` \\((.*?)\\) ENGINE", Pattern.DOTALL)
            .matcher(resource("V1__initial_schema.sql"));
    var column = Pattern.compile("(?m)^\\s*`([^`]+)`\\s+(\\w+)");
    while (create.find()) {
      var columns = new LinkedHashMap<String, Column>();
      var match = column.matcher(create.group(2));
      while (match.find()) columns.put(match.group(1), new Column(match.group(1), match.group(2)));
      assertNull(tables.put(create.group(1), columns), "Duplicate migration table");
    }
    var alter =
        Pattern.compile("ALTER TABLE\\s+`?(\\w+)`?\\s+(.*?);", Pattern.DOTALL)
            .matcher(resource("V3__video_media.sql"));
    while (alter.find()) {
      var added = Pattern.compile("ADD COLUMN\\s+`?(\\w+)`?\\s+(\\w+)").matcher(alter.group(2));
      while (added.find()) {
        var definition = new Column(added.group(1), added.group(2));
        assertNull(tables.get(alter.group(1)).put(definition.name(), definition));
      }
    }
    return tables;
  }

  private static String resource(String migration) throws Exception {
    try (var input =
        EntitySchemaContractTest.class.getResourceAsStream("/db/migration/" + migration)) {
      assertNotNull(input, migration);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  public static Map<String, Class<?>> entities() throws Exception {
    var result = new TreeMap<String, Class<?>>();
    for (var imported : new ClassFileImporter().importPackages("cn.shiguang.notes.entity")) {
      Class<?> type = Class.forName(imported.getName());
      TableName table = type.getAnnotation(TableName.class);
      if (table != null)
        assertNull(result.put(table.value(), type), "Duplicate @TableName: " + table.value());
    }
    return result;
  }

  public static Map<String, Field> fields(Class<?> entity) {
    Map<String, Field> result = new LinkedHashMap<>();
    for (Class<?> type = entity; type != Object.class; type = type.getSuperclass()) {
      for (Field field : type.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
        TableField annotation = field.getAnnotation(TableField.class);
        if (annotation != null && !annotation.exist()) continue;
        String name =
            annotation != null && !annotation.value().isEmpty()
                ? annotation.value()
                : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        assertNull(result.put(name, field), "Duplicate persistent column " + name);
      }
    }
    return result;
  }

  public static Class<?> mapper(Class<?> entity) throws Exception {
    return Class.forName(
        "cn.shiguang.notes.mapper." + entity.getSimpleName().replaceFirst("Entity$", "Mapper"));
  }

  @Test
  void everyMigrationTableHasACompleteTypedJavaBeanAndMatchingBaseMapper() throws Exception {
    var expected = schema();
    var actual = entities();
    assertEquals(
        30,
        expected.size(),
        "Business schema unexpectedly changed; review the persistence contract");
    assertEquals(
        expected.keySet(), actual.keySet(), "Every business table needs exactly one entity");
    for (var table : expected.entrySet()) {
      Class<?> entity = actual.get(table.getKey());
      assertEquals("BaseEntity", entity.getSuperclass().getSimpleName(), entity.getName());
      assertNotNull(entity.getConstructor());
      var fields = fields(entity);
      assertEquals(table.getValue().keySet(), fields.keySet(), entity.getName() + " columns");
      var properties = new HashMap<String, java.beans.PropertyDescriptor>();
      for (var property : Introspector.getBeanInfo(entity).getPropertyDescriptors())
        properties.put(property.getName(), property);
      for (var column : table.getValue().values()) {
        Field field = fields.get(column.name());
        String label = table.getKey() + "." + column.name();
        assertEquals(column.javaType(), field.getType(), label);
        assertTrue(Modifier.isPrivate(field.getModifiers()), label + " must be encapsulated");
        assertNull(
            field.getAnnotation(Version.class),
            label + " uses explicit business CAS, not automatic @Version");
        var property = properties.get(field.getName());
        assertNotNull(property, label + " JavaBean property");
        assertNotNull(property.getReadMethod(), label + " getter");
        assertNotNull(property.getWriteMethod(), label + " setter");
      }
      TableId id = fields.get("id").getAnnotation(TableId.class);
      assertNotNull(id, entity.getName() + " @TableId");
      assertEquals(IdType.AUTO, id.type());
      Class<?> mapper = mapper(entity);
      assertTrue(mapper.isInterface());
      assertEquals(
          entity,
          ResolvableType.forClass(mapper).as(BaseMapper.class).getGeneric(0).resolve(),
          mapper.getName());
      assertNotNull(mapper.getAnnotation(org.apache.ibatis.annotations.Mapper.class));
    }
  }
}
