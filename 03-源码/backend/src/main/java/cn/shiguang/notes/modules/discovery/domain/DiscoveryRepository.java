package cn.shiguang.notes.modules.discovery.domain;

import java.util.*;

public interface DiscoveryRepository {
  List<Map<String, Object>> candidates(
      long user, String mode, String category, String keyword, Long topic, Long author);

  Set<String> visible(long user, List<String> ids);

  Map<String, Object> preferences(long user);

  void preferences(long user, List<Long> likes, List<Long> reduced);

  void hide(long user, long note, boolean hidden);

  void reset(long user);
}
