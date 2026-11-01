package cn.shiguang.notes.workflows;

import cn.shiguang.notes.modules.collection.api.CollectionApi;
import cn.shiguang.notes.modules.content.api.ContentApi;
import cn.shiguang.notes.modules.discovery.api.DiscoveryApi;
import cn.shiguang.notes.modules.governance.api.GovernanceApi;
import cn.shiguang.notes.modules.notification.api.NotificationApi;
import cn.shiguang.notes.modules.social.api.SocialApi;
import cn.shiguang.notes.modules.statistics.api.StatisticsApi;
import cn.shiguang.notes.platform.application.Pages;
import cn.shiguang.notes.platform.readmodel.PublicReadModel;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authorized query composition, keyset paging and privacy projections. */
@Service
public class QueryUseCases {
  private final ContentApi content;
  private final CollectionApi collections;
  private final SocialApi social;
  private final GovernanceApi governance;
  private final DiscoveryApi discovery;
  private final NotificationApi notifications;
  private final StatisticsApi statistics;
  private final Pages pages;
  private final PublicReadModel read;

  public QueryUseCases(
      ContentApi content,
      CollectionApi collections,
      SocialApi social,
      GovernanceApi governance,
      DiscoveryApi discovery,
      NotificationApi notifications,
      StatisticsApi statistics,
      Pages pages,
      PublicReadModel read) {
    this.content = content;
    this.collections = collections;
    this.social = social;
    this.governance = governance;
    this.discovery = discovery;
    this.notifications = notifications;
    this.statistics = statistics;
    this.pages = pages;
    this.read = read;
  }

  public Object feed(Actor actor, Map<String, String> query) {
    return discovery.feed(actor, query);
  }

  @Transactional
  public Object note(Actor actor, long id, String subject) {
    var n = content.detail(id, actor, false);
    n.putAll(read.interaction(actor.id(), id));
    statistics.viewed(actor.id(), subject, id, Data.id(n, "authorId"));
    return n;
  }

  public Object ownNote(Actor actor, long id) {
    actor.requireLogin();
    return content.detail(id, actor, true);
  }

  public Object ownNotes(Actor a, String cursor, int size) {
    a.requireLogin();
    return pages.fetch(
        "own-notes",
        a.id(),
        cursor,
        size,
        (b, s) ->
            content.owned(a.id(), b, s).stream()
                .map(n -> content.preview(Data.id(n, "id")))
                .toList());
  }

  public Object comments(Actor a, long note, Long root, String cursor, int size) {
    content.requirePublic(note, false);
    return pages.fetch(
        "comments:" + note + ":" + root,
        a.id(),
        cursor,
        size,
        (b, s) -> social.comments(note, root, b, s));
  }

  public Object replies(Actor a, long comment, String cursor, int size) {
    var c = social.comment(comment, false);
    long root = c.get("rootId") == null ? comment : Data.id(c, "rootId");
    return comments(a, Data.id(c, "noteId"), root, cursor, size);
  }

  public Object follows(Actor a, long user, boolean followers, String cursor, int size) {
    return pages.fetch(
        "follows:" + user + ":" + followers,
        a.id(),
        cursor,
        size,
        "relationId",
        (b, s) -> social.follows(user, followers, b, s));
  }

  public Object folders(Actor a, String cursor, int size) {
    a.requireLogin();
    return pages.fetch("folders", a.id(), cursor, size, (b, s) -> collections.folders(a, b, s));
  }

  @SuppressWarnings("unchecked")
  public Object favorites(Actor a, long folder, String cursor, int size) {
    a.requireLogin();
    var result =
        pages.fetch(
            "favorites:" + folder,
            a.id(),
            cursor,
            size,
            (b, s) -> collections.favorites(a, folder, b, s));
    for (var row : (List<Map<String, Object>>) result.get("items")) {
      long note = Data.id(row, "noteId");
      boolean available = read.noteVisible(note);
      row.put("available", available);
      row.put("title", available ? content.note(note, false).get("title") : null);
    }
    return result;
  }

  private Map<String, Object> practiceDto(Map<String, Object> p) {
    var dto =
        Data.select(
            p, "id", "noteId", "status", "version", "publicStatus", "resultText", "helpfulness");
    dto.put("mediaIds", p.getOrDefault("mediaIds", List.of()));
    dto.put("sourceAvailable", read.noteVisible(Data.id(p, "noteId")));
    return dto;
  }

  public Object practice(Actor a, long id) {
    a.requireLogin();
    return practiceDto(collections.ownPractice(a, id, false));
  }

  public Object practices(Actor a, String cursor, int size) {
    a.requireLogin();
    return pages.fetch(
        "practices",
        a.id(),
        cursor,
        size,
        (b, s) -> collections.practices(a, b, s).stream().map(this::practiceDto).toList());
  }

  public Object history(Actor a, long id, String cursor, int size) {
    a.requireLogin();
    return pages.fetch(
        "practice-history:" + id, a.id(), cursor, size, (b, s) -> collections.history(a, id, b, s));
  }

  public Object feedback(Actor a, long note, String cursor, int size) {
    content.requirePublic(note, false);
    return pages.fetch(
        "feedback:" + note,
        a.id(),
        cursor,
        size,
        (b, s) ->
            read.feedback(note, b, s).stream()
                .map(
                    p -> {
                      p.put(
                          "mediaIds",
                          collections.practice(Data.id(p, "id"), false).get("mediaIds"));
                      return p;
                    })
                .toList());
  }

  public Object preferences(Actor a) {
    a.requireLogin();
    return discovery.preferences(a.id());
  }

  public Object reviews(Actor a, String status, String cursor, int size) {
    a.require("REVIEWER");
    return pages.fetch(
        "reviews:" + status, a.id(), cursor, size, (b, s) -> governance.reviews(status, b, s));
  }

  public Object review(Actor a, long id) {
    a.require("REVIEWER");
    var r = governance.review(id, false);
    if (Data.id(r, "applicantId") == a.id()) throw BizException.forbidden();
    boolean note = r.get("targetType").equals("NOTE");
    var p =
        note
            ? content.preview(Data.id(r, "targetId"))
            : practiceDto(collections.practice(Data.id(r, "targetId"), false));
    int revision =
        note
            ? Data.number(p, "revision")
            : Data.number(collections.practice(Data.id(r, "targetId"), false), "feedbackRevision");
    if (revision != Data.number(r, "targetRevision") || !"PENDING".equals(r.get("status")))
      throw BizException.conflict("审核预览已过期");
    return Data.map(
        "review",
        Data.select(
            r,
            "id",
            "targetType",
            "targetId",
            "targetRevision",
            "version",
            "status",
            "applicantId"),
        "note",
        note ? p : null,
        "practice",
        note ? null : p);
  }

  public Object reports(Actor a, boolean admin, String cursor, int size) {
    a.requireLogin();
    if (admin) a.require("REVIEWER");
    return pages.fetch(
        "reports:" + admin,
        a.id(),
        cursor,
        size,
        (b, s) -> governance.reports(admin ? null : a.id(), b, s));
  }

  public Object topics(Actor a, String cursor, int size) {
    return pages.fetch("topics", a.id(), cursor, size, (b, s) -> governance.topics(b, s));
  }

  public Object adminTopics(Actor a, String cursor, int size) {
    a.require("ADMIN");
    return pages.fetch("admin-topics", a.id(), cursor, size, (b, s) -> governance.allTopics(b, s));
  }

  public Object audits(Actor a, String cursor, int size) {
    a.requireLogin();
    if (!a.has("REVIEWER") && !a.has("ADMIN")) throw BizException.forbidden();
    return pages.fetch("audits", a.id(), cursor, size, (b, s) -> governance.audits(b, s));
  }

  @SuppressWarnings("unchecked")
  public Object notifications(Actor a, String type, String cursor, int size) {
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
    return result;
  }

  public Object unread(Actor a) {
    a.requireLogin();
    return Map.of("count", notifications.unread(a.id()));
  }

  public Object dashboard(Actor a, String scope, int days) {
    return statistics.dashboard(a, scope, days);
  }

  public Object ranking(Actor a, boolean platform, String cursor, int size) {
    return pages.fetch(
        "ranking:" + platform,
        a.id(),
        cursor,
        size,
        "position",
        (b, s) -> statistics.ranking(a, platform, b, s));
  }

  public Object users(Actor a, String cursor, int size) {
    a.require("ADMIN");
    return pages.fetch("users", a.id(), cursor, size, (b, s) -> read.users(b, s));
  }
}
