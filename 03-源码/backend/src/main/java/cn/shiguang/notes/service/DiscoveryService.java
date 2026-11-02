package cn.shiguang.notes.service;

import cn.shiguang.notes.dto.Actor;
import java.util.*;

public interface DiscoveryService {
  Map<String, Object> feed(Actor actor, Map<String, String> query);

  Map<String, Object> preferences(long user);

  void preferences(long user, Map<String, Object> body);

  void hide(long user, long note, boolean hidden);

  void reset(long user);

  void clear(long user);
}
