package cn.shiguang.notes.modules.notification.api;

import java.util.*;

public interface NotificationApi {
  void receive(Map<String, Object> event);

  List<Map<String, Object>> list(long user, String type, long before, int size);

  long unread(long user);

  void read(long user, long id);

  void readAll(long user, long through);
}
