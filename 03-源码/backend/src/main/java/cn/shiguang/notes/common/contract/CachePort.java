package cn.shiguang.notes.common.contract;

public interface CachePort {
  String get(String key);

  boolean put(String key, String value, int seconds);

  void removePrefix(String prefix);
}
