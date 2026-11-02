package cn.shiguang.notes.service;

import cn.shiguang.notes.dto.Actor;
import java.util.*;

public interface SocialService {
  boolean like(long user, long note, boolean desired);

  boolean follow(long from, long to, boolean desired);

  Map<String, Object> comment(long id, boolean lock);

  long createComment(Actor actor, long note, Map<String, Object> input);

  void deleteComment(Actor actor, long id);

  void hideComment(long id, int version);

  List<Map<String, Object>> comments(long note, Long root, long before, int size);

  List<Map<String, Object>> follows(long user, boolean followers, long before, int size);
}
