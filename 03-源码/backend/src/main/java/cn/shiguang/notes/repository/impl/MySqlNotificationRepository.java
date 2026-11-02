package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.NotificationRepository;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlNotificationRepository implements NotificationRepository {
  private final NotificationMapper notificationMapper;

  public MySqlNotificationRepository(NotificationMapper notificationMapper) {
    this.notificationMapper = notificationMapper;
  }

  public void insert(
      long u, String event, String type, long actor, String targetType, long target) {
    notificationMapper.notificationInsert(u, event, type, actor, targetType, target);
  }

  public List<Map<String, Object>> list(long u, String type, long before, int size) {
    return type == null || type.isBlank()
        ? MapperRows.list(notificationMapper.notificationList(u, before, size))
        : MapperRows.list(notificationMapper.notificationList2(u, type, before, size));
  }

  public long unread(long u) {
    return notificationMapper.notificationUnread(u);
  }

  public void read(long u, long id) {
    if (notificationMapper.notificationRead(id, u) == 0) throw BizException.missing();
    notificationMapper.notificationRead2(id, u);
  }

  public void readAll(long u, long through) {
    notificationMapper.notificationReadAll(u, through);
  }
}
