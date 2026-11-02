package cn.shiguang.notes.common.util;

import cn.shiguang.notes.common.exception.BizException;
import java.util.*;

/** Safe DTO parsing and explicit public projection. */
public final class Data {
  private Data() {}

  public static String text(Map<?, ?> data, String key) {
    Object v = data.get(key);
    return v == null ? "" : v.toString();
  }

  public static long id(Map<?, ?> data, String key) {
    try {
      return Long.parseLong(text(data, key));
    } catch (NumberFormatException e) {
      throw BizException.invalid("无效的" + key);
    }
  }

  public static int number(Map<?, ?> data, String key) {
    try {
      return Integer.parseInt(text(data, key));
    } catch (NumberFormatException e) {
      throw BizException.invalid("缺少或无效的" + key);
    }
  }

  public static List<Long> ids(Map<?, ?> data, String key) {
    Object raw = data.get(key);
    if (!(raw instanceof List<?> list)) throw BizException.invalid("缺少" + key);
    if (list.size() > 30) throw BizException.invalid("列表过长");
    try {
      var ids = list.stream().map(v -> Long.valueOf(v.toString())).toList();
      if (new HashSet<>(ids).size() != ids.size()) throw BizException.invalid("列表不能重复");
      return ids;
    } catch (NumberFormatException e) {
      throw BizException.invalid("无效ID列表");
    }
  }

  public static Map<String, Object> select(Map<String, Object> row, String... keys) {
    var result = new LinkedHashMap<String, Object>();
    for (String key : keys) result.put(key, row.get(key));
    return result;
  }

  public static Map<String, Object> map(Object... pairs) {
    var r = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) r.put(pairs[i].toString(), pairs[i + 1]);
    return r;
  }
}
