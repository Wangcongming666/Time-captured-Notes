package cn.shiguang.notes.modules.notification.domain;

import java.util.*;

public interface NotificationRepository {
  void insert(long user, String event, String type, long actor, String targetType, long target);

  List<Map<String, Object>> list(long user, String type, long before, int size);

  long unread(long user);

  void read(long user, long id);

  void readAll(long user, long through);
}
