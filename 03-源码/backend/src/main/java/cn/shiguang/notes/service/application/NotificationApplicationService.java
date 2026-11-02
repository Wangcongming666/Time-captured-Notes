package cn.shiguang.notes.service.application;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.repository.query.PublicQueryRepository;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.service.support.*;
import cn.shiguang.notes.vo.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification use cases: authorization, transaction orchestration, and typed public projections.
 */
@Service
public class NotificationApplicationService {
  private final NotificationService notifications;
  private final Pages pages;
  private final PublicQueryRepository read;
  private final HttpBoundaryConverter views;

  public NotificationApplicationService(
      NotificationService notifications,
      Pages pages,
      PublicQueryRepository read,
      HttpBoundaryConverter views) {
    this.notifications = notifications;
    this.pages = pages;
    this.read = read;
    this.views = views;
  }

  @Transactional
  public void readNotification(Actor actor, long id) {
    actor.requireLogin();
    notifications.read(actor.id(), id);
  }

  @Transactional
  public void readAll(Actor actor, ReadAllRequest request) {
    long through = Data.id(views.input(request), "throughId");
    actor.requireLogin();
    notifications.readAll(actor.id(), through);
  }

  @SuppressWarnings("unchecked")
  public PageVO<NotificationVO> notifications(Actor a, String type, String cursor, int size) {
    a.requireLogin();
    var result =
        pages.fetch(
            "notifications:" + type,
            a.id(),
            cursor,
            size,
            (b, s) -> notifications.list(a.id(), type, b, s));
    for (var n : (List<Map<String, Object>>) result.get("items"))
      n.put(
          "targetAvailable",
          read.targetVisible(Data.text(n, "targetType"), Data.id(n, "targetId")));
    return views.page(result, NotificationVO.class);
  }

  public UnreadVO unread(Actor a) {
    a.requireLogin();
    return views.view(Map.of("count", notifications.unread(a.id())), UnreadVO.class);
  }
}
