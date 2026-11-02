package cn.shiguang.notes.converter;

import cn.shiguang.notes.vo.PageVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Bridges legacy application maps to named transport schemas without exposing unknown attributes.
 */
@Component
public final class HttpBoundaryConverter {
  private final ObjectMapper json;

  public HttpBoundaryConverter(ObjectMapper json) {
    this.json = json.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public Map<String, Object> input(Object request) {
    Map<String, Object> values =
        json.convertValue(request, new TypeReference<LinkedHashMap<String, Object>>() {});
    values.values().removeIf(Objects::isNull);
    return values;
  }

  public Map<String, String> query(Object request) {
    var result = new LinkedHashMap<String, String>();
    input(request).forEach((key, value) -> result.put(key, value.toString()));
    return result;
  }

  public <T> T view(Object data, Class<T> type) {
    return json.convertValue(data, type);
  }

  public <T> PageVO<T> page(Object data, Class<T> itemType) {
    return json.convertValue(
        data, json.getTypeFactory().constructParametricType(PageVO.class, itemType));
  }
}
