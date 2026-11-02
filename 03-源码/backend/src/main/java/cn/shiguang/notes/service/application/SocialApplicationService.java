package cn.shiguang.notes.service.application;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.service.support.*;
import cn.shiguang.notes.vo.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Social use cases: authorization, transaction orchestration, and typed public projections. */
@Service
public class SocialApplicationService {
  private final IdentityService identity;
  private final ContentService content;
  private final SocialService social;
  private final DiscoveryService discovery;
  private final EventPort events;
  private final Commands commands;
  private final Pages pages;
  private final HttpBoundaryConverter views;

  public SocialApplicationService(
      IdentityService identity,
      ContentService content,
      SocialService social,
      DiscoveryService discovery,
      EventPort events,
      Commands commands,
      Pages pages,
      HttpBoundaryConverter views) {
    this.identity = identity;
    this.content = content;
    this.social = social;
    this.discovery = discovery;
    this.events = events;
    this.commands = commands;
    this.pages = pages;
    this.views = views;
  }

  @Transactional
  public void like(Actor actor, long id, boolean desired) {
    actor.requireLogin();
    var n = desired ? content.requirePublic(id, true) : content.note(id, true);
    if (social.like(actor.id(), id, desired)) {
      var p = notePayload(n);
      p.put("desired", desired);
      p.put("recipientId", n.get("authorId"));
      p.put("notificationType", "LIKE");
      events.append(
          "LikeChanged",
          actor.id(),
          "NOTE",
          id,
          0,
          p,
          desired
              ? new String[] {"notification", "statistics", "cache"}
              : new String[] {"statistics", "cache"});
    }
  }

  @Transactional
  public IdVO comment(Actor actor, long note, CommentRequest request, String key) {
    var body = views.input(request);
    return views.view(
        commands.run(
            actor,
            "comment:create:" + note,
            key,
            body,
            () -> {
              var n = content.requirePublic(note, true);
              long id = social.createComment(actor, note, body);
              var p = notePayload(n);
              p.put("recipientId", n.get("authorId"));
              p.put("notificationType", "COMMENT");
              events.append(
                  "CommentCreated",
                  actor.id(),
                  "NOTE",
                  note,
                  0,
                  p,
                  "notification",
                  "statistics",
                  "cache");
              return Map.of("id", Long.toString(id));
            }),
        IdVO.class);
  }

  @Transactional
  public void deleteComment(Actor actor, long id) {
    actor.requireLogin();
    var c = social.comment(id, false);
    content.note(Data.id(c, "noteId"), true);
    social.deleteComment(actor, id);
    changed(
        actor,
        "CommentVisibilityChanged",
        "NOTE",
        Data.id(c, "noteId"),
        Data.map("noteId", c.get("noteId")));
  }

  @Transactional
  public void follow(Actor actor, long id, boolean desired) {
    actor.requireLogin();
    identity.profile(id, false);
    if (social.follow(actor.id(), id, desired)) {
      events.append(
          "FollowChanged",
          actor.id(),
          "USER",
          id,
          0,
          Data.map(
              "recipientId", Long.toString(id), "notificationType", "FOLLOW", "desired", desired),
          desired
              ? new String[] {"notification", "statistics", "cache"}
              : new String[] {"statistics", "cache"});
      discovery.clear(actor.id());
    }
  }

  public PageVO<CommentVO> comments(Actor a, long note, Long root, String cursor, int size) {
    content.requirePublic(note, false);
    return views.page(
        pages.fetch(
            "comments:" + note + ":" + root,
            a.id(),
            cursor,
            size,
            (b, s) -> social.comments(note, root, b, s)),
        CommentVO.class);
  }

  public PageVO<CommentVO> replies(Actor a, long comment, String cursor, int size) {
    var c = social.comment(comment, false);
    long root = c.get("rootId") == null ? comment : Data.id(c, "rootId");
    return views.page(comments(a, Data.id(c, "noteId"), root, cursor, size), CommentVO.class);
  }

  public PageVO<FollowVO> follows(Actor a, long user, boolean followers, String cursor, int size) {
    return views.page(
        pages.fetch(
            "follows:" + user + ":" + followers,
            a.id(),
            cursor,
            size,
            "relationId",
            (b, s) -> social.follows(user, followers, b, s)),
        FollowVO.class);
  }

  private void changed(
      Actor actor, String type, String aggregate, long id, Map<String, Object> payload) {
    events.append(type, actor.id(), aggregate, id, 0, payload, "statistics", "cache");
  }

  private Map<String, Object> notePayload(Map<String, Object> note) {
    return Data.map("noteId", note.get("id"), "authorId", note.get("authorId"));
  }
}
