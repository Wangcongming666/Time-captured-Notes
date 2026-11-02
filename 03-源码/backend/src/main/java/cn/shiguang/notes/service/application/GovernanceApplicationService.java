package cn.shiguang.notes.service.application;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.exception.BizException;
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

/** Governance use cases: authorization, transaction orchestration, and typed public projections. */
@Service
public class GovernanceApplicationService {
  private final ContentService content;
  private final CollectionService collections;
  private final SocialService social;
  private final GovernanceService governance;
  private final EventPort events;
  private final Commands commands;
  private final Pages pages;
  private final PublicQueryRepository read;
  private final HttpBoundaryConverter views;

  public GovernanceApplicationService(
      ContentService content,
      CollectionService collections,
      SocialService social,
      GovernanceService governance,
      EventPort events,
      Commands commands,
      Pages pages,
      PublicQueryRepository read,
      HttpBoundaryConverter views) {
    this.content = content;
    this.collections = collections;
    this.social = social;
    this.governance = governance;
    this.events = events;
    this.commands = commands;
    this.pages = pages;
    this.read = read;
    this.views = views;
  }

  @Transactional
  public VersionVO decideReview(Actor actor, long id, ReviewDecisionRequest request, String key) {
    var body = views.input(request);
    actor.require("REVIEWER");
    return views.view(
        commands.run(
            actor,
            "review:decision:" + id,
            key,
            body,
            () -> {
              var before = governance.review(id, false);
              String type = Data.text(before, "targetType");
              long target = Data.id(before, "targetId");
              int revision = Data.number(before, "targetRevision");
              String decision = Data.text(body, "decision"), reason = Data.text(body, "reason");
              Map<String, Object> note;
              if (type.equals("NOTE")) {
                note = content.note(target, true);
              } else {
                var initial = collections.practice(target, false);
                note = content.requirePublic(Data.id(initial, "noteId"), true);
                var p = collections.practice(target, true);
                if (Data.number(p, "sourceRevision") != Data.number(note, "revision"))
                  throw BizException.conflict("源笔记版本已改变");
              }
              governance.decide(actor, id, Data.number(body, "version"), decision, reason);
              if (type.equals("NOTE")) content.moderate(target, revision, decision, reason);
              else collections.moderate(target, revision, decision);
              governance.audit(
                  actor,
                  "REVIEW_" + decision,
                  type,
                  target,
                  Data.select(before, "status", "targetRevision"),
                  Data.map("decision", decision),
                  reason);
              var payload = notePayload(note);
              payload.put("decision", decision);
              payload.put("recipientId", before.get("applicantId"));
              payload.put("notificationType", "REVIEW");
              events.append(
                  "ReviewDecided",
                  actor.id(),
                  type,
                  target,
                  revision,
                  payload,
                  "notification",
                  "statistics",
                  "cache");
              return version(governance.review(id, false));
            }),
        VersionVO.class);
  }

  @Transactional
  public void offlineNote(Actor actor, long id, ModerationRequest request) {
    var body = views.input(request);
    actor.require("REVIEWER");
    String reason = reason(body);
    var n = content.note(id, true);
    content.offline(id, Data.number(body, "version"), reason);
    governance.audit(
        actor,
        "NOTE_OFFLINE",
        "NOTE",
        id,
        Data.select(n, "status"),
        Map.of("status", "OFFLINE"),
        reason);
    events.append(
        "NoteVisibilityChanged",
        actor.id(),
        "NOTE",
        id,
        Data.number(n, "revision"),
        Data.map(
            "noteId",
            Long.toString(id),
            "authorId",
            n.get("authorId"),
            "recipientId",
            n.get("authorId"),
            "notificationType",
            "REVIEW"),
        "notification",
        "statistics",
        "cache");
  }

  @Transactional
  public void hideComment(Actor actor, long id, ModerationRequest request) {
    var body = views.input(request);
    actor.require("REVIEWER");
    String reason = reason(body);
    var c = social.comment(id, false);
    content.note(Data.id(c, "noteId"), true);
    social.hideComment(id, Data.number(body, "version"));
    governance.audit(
        actor,
        "COMMENT_HIDE",
        "COMMENT",
        id,
        Data.select(c, "status"),
        Map.of("status", "HIDDEN"),
        reason);
    changed(
        actor,
        "CommentVisibilityChanged",
        "NOTE",
        Data.id(c, "noteId"),
        Data.map("noteId", c.get("noteId")));
  }

  @Transactional
  public void offlineFeedback(Actor actor, long id, ModerationRequest request) {
    var body = views.input(request);
    actor.require("REVIEWER");
    var p = collections.practice(id, true);
    collections.offline(id, Data.number(body, "version"));
    governance.audit(
        actor,
        "FEEDBACK_OFFLINE",
        "PRACTICE",
        id,
        Data.select(p, "publicStatus"),
        Map.of("publicStatus", "OFFLINE"),
        reason(body));
    changed(actor, "FeedbackVisibilityChanged", "PRACTICE", id, Map.of());
  }

  @Transactional
  public IdVO report(Actor actor, ReportRequest request, String key) {
    var body = views.input(request);
    return views.view(
        commands.run(
            actor,
            "report:create",
            key,
            body,
            () -> {
              String type = Data.text(body, "targetType");
              long target = Data.id(body, "targetId");
              if (!read.targetVisible(type, target)) throw BizException.missing();
              return Map.of(
                  "id", Long.toString(governance.report(actor, type, target, reason(body))));
            }),
        IdVO.class);
  }

  @Transactional
  public VersionVO decideReport(Actor actor, long id, ReportDecisionRequest request, String key) {
    var body = views.input(request);
    actor.require("REVIEWER");
    return views.view(
        commands.run(
            actor,
            "report:decision:" + id,
            key,
            body,
            () -> {
              var report = governance.report(id, false);
              String type = Data.text(report, "targetType"), status = Data.text(body, "status");
              long target = Data.id(report, "targetId");
              if (status.equals("UPHELD") && read.targetVisible(type, target)) {
                if (type.equals("NOTE")) {
                  var n = content.note(target, true);
                  content.offline(target, Data.number(n, "version"), reason(body));
                  changed(actor, "NoteVisibilityChanged", "NOTE", target, notePayload(n));
                } else if (type.equals("COMMENT")) {
                  var c = social.comment(target, false);
                  content.note(Data.id(c, "noteId"), true);
                  social.hideComment(target, Data.number(c, "version"));
                  changed(
                      actor,
                      "CommentVisibilityChanged",
                      "NOTE",
                      Data.id(c, "noteId"),
                      Data.map("noteId", c.get("noteId")));
                } else {
                  var p = collections.practice(target, true);
                  collections.offline(target, Data.number(p, "version"));
                }
              }
              governance.decideReport(
                  actor, id, Data.number(body, "version"), status, reason(body));
              governance.audit(
                  actor,
                  "REPORT_" + status,
                  "REPORT",
                  id,
                  Data.select(report, "status"),
                  Map.of("status", status),
                  reason(body));
              if (!status.equals("PROCESSING"))
                events.append(
                    "ReportDecided",
                    actor.id(),
                    "REPORT",
                    id,
                    0,
                    Data.map("recipientId", report.get("reporterId"), "notificationType", "REPORT"),
                    "notification");
              return version(governance.report(id, false));
            }),
        VersionVO.class);
  }

  public PageVO<ReviewVO> reviews(Actor a, String status, String cursor, int size) {
    a.require("REVIEWER");
    return views.page(
        pages.fetch(
            "reviews:" + status, a.id(), cursor, size, (b, s) -> governance.reviews(status, b, s)),
        ReviewVO.class);
  }

  public ReviewDetailVO review(Actor a, long id) {
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
    return views.view(
        Data.map(
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
            note ? null : p),
        ReviewDetailVO.class);
  }

  public PageVO<ReportVO> reports(Actor a, boolean admin, String cursor, int size) {
    a.requireLogin();
    if (admin) a.require("REVIEWER");
    return views.page(
        pages.fetch(
            "reports:" + admin,
            a.id(),
            cursor,
            size,
            (b, s) -> governance.reports(admin ? null : a.id(), b, s)),
        ReportVO.class);
  }

  private Map<String, Object> practiceDto(Map<String, Object> p) {
    var dto =
        Data.select(
            p, "id", "noteId", "status", "version", "publicStatus", "resultText", "helpfulness");
    dto.put("mediaIds", p.getOrDefault("mediaIds", List.of()));
    dto.put("sourceAvailable", read.noteVisible(Data.id(p, "noteId")));
    return dto;
  }

  private Map<String, Object> version(Map<String, Object> row) {
    return Data.select(row, "id", "version", "status");
  }

  private void changed(
      Actor actor, String type, String aggregate, long id, Map<String, Object> payload) {
    events.append(type, actor.id(), aggregate, id, 0, payload, "statistics", "cache");
  }

  private Map<String, Object> notePayload(Map<String, Object> note) {
    return Data.map("noteId", note.get("id"), "authorId", note.get("authorId"));
  }

  private String reason(Map<String, Object> body) {
    String r = Data.text(body, "reason");
    if (r.isBlank() || r.length() > 500) throw BizException.invalid("请填写1–500字理由");
    return r;
  }
}
