package cn.shiguang.notes.service.impl;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.repository.NotificationRepository;
import cn.shiguang.notes.service.NotificationService;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
  private final NotificationRepository repo;

  public NotificationServiceImpl(NotificationRepository repo) {
    this.repo = repo;
  }

  @SuppressWarnings("unchecked")
  public void receive(Map<String, Object> event) {
    var p = (Map<String, Object>) event.get("payload");
    if (p == null || p.get("recipientId") == null) return;
    long recipient = Data.id(p, "recipientId"), actor = Data.id(event, "actorId");
    if (recipient == actor) return;
    repo.insert(
        recipient,
        Data.text(event, "eventId"),
        Data.text(p, "notificationType"),
        actor,
        Data.text(event, "aggregateType"),
        Data.id(event, "aggregateId"));
  }

  public List<Map<String, Object>> list(long u, String t, long b, int s) {
    return repo.list(u, t, b, s);
  }

  public long unread(long u) {
    return repo.unread(u);
  }

  public void read(long u, long id) {
    repo.read(u, id);
  }

  public void readAll(long u, long id) {
    repo.readAll(u, id);
  }
}
