package cn.shiguang.notes.repository;

import java.util.*;

public interface SocialRepository {
  boolean like(long user, long note, boolean desired);

  boolean follow(long from, long to, boolean desired);

  Map<String, Object> comment(long id, boolean lock);

  long createComment(long author, long note, Long root, Long reply, String body);

  void state(long id, String state, int version);

  List<Map<String, Object>> comments(long note, Long root, long before, int size);

  List<Map<String, Object>> follows(long user, boolean followers, long before, int size);
}
