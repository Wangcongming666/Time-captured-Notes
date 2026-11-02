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

/** Practice use cases: authorization, transaction orchestration, and typed public projections. */
@Service
public class PracticeApplicationService {
  private final ContentService content;
  private final CollectionService collections;
  private final GovernanceService governance;
  private final EventPort events;
  private final Commands commands;
  private final Pages pages;
  private final PublicQueryRepository read;
  private final HttpBoundaryConverter views;

  public PracticeApplicationService(
      ContentService content,
      CollectionService collections,
      GovernanceService governance,
      EventPort events,
      Commands commands,
      Pages pages,
      PublicQueryRepository read,
      HttpBoundaryConverter views) {
    this.content = content;
    this.collections = collections;
    this.governance = governance;
    this.events = events;
    this.commands = commands;
    this.pages = pages;
    this.read = read;
    this.views = views;
  }

  @Transactional
  public VersionVO createPractice(Actor actor, CreatePracticeRequest request, String key) {
    var body = views.input(request);
    return views.view(
        commands.run(
            actor,
            "practice:create",
            key,
            body,
            () -> {
              long note = Data.id(body, "noteId");
              content.requirePublic(note, true);
              long id = collections.createPractice(actor, note);
              changed(actor, "PracticeChanged", "PRACTICE", id, Map.of());
              return version(collections.practice(id, false));
            }),
        VersionVO.class);
  }

  @Transactional
  public VersionVO transition(Actor actor, long id, PracticeTransitionRequest request, String key) {
    var body = views.input(request);
    return views.view(
        commands.run(
            actor,
            "practice:transition:" + id,
            key,
            body,
            () -> {
              var p =
                  collections.transition(
                      actor, id, Data.number(body, "version"), Data.text(body, "status"));
              changed(actor, "PracticeChanged", "PRACTICE", id, Map.of());
              return version(p);
            }),
        VersionVO.class);
  }

  @Transactional
  public VersionVO result(Actor actor, long id, PracticeResultRequest request) {
    var body = views.input(request);
    actor.requireLogin();
    var p = collections.result(actor, id, body);
    var media = Data.ids(body, "mediaIds");
    if (media.size() > 9) throw BizException.invalid("最多9张图片");
    content.bind(actor, media, "PRACTICE", "PRACTICE", id);
    governance.cancelReviews("PRACTICE", id);
    changed(actor, "FeedbackVisibilityChanged", "PRACTICE", id, Map.of());
    return views.view(version(p), VersionVO.class);
  }

  @Transactional
  public VersionVO publishFeedback(Actor actor, long id, VersionRequest request, String key) {
    int v = request.version();
    return views.view(
        commands.run(
            actor,
            "practice:publish:" + id,
            key,
            Data.map("version", v),
            () -> {
              var initial = collections.ownPractice(actor, id, false);
              var n = content.requirePublic(Data.id(initial, "noteId"), true);
              var p = collections.publishRequest(actor, id, v, Data.number(n, "revision"));
              governance.cancelReviews("PRACTICE", id);
              governance.openReview("PRACTICE", id, Data.number(p, "feedbackRevision"), actor.id());
              return version(p);
            }),
        VersionVO.class);
  }

  @Transactional
  public VersionVO unpublishFeedback(Actor actor, long id, VersionRequest request, String key) {
    int v = request.version();
    return views.view(
        commands.run(
            actor,
            "practice:unpublish:" + id,
            key,
            Data.map("version", v),
            () -> {
              var p = collections.unpublish(actor, id, v);
              governance.cancelReviews("PRACTICE", id);
              changed(actor, "FeedbackVisibilityChanged", "PRACTICE", id, Map.of());
              return version(p);
            }),
        VersionVO.class);
  }

  public PracticeVO practice(Actor a, long id) {
    a.requireLogin();
    return views.view(practiceDto(collections.ownPractice(a, id, false)), PracticeVO.class);
  }

  public PageVO<PracticeVO> practices(Actor a, String cursor, int size) {
    a.requireLogin();
    return views.page(
        pages.fetch(
            "practices",
            a.id(),
            cursor,
            size,
            (b, s) -> collections.practices(a, b, s).stream().map(this::practiceDto).toList()),
        PracticeVO.class);
  }

  public PageVO<HistoryVO> history(Actor a, long id, String cursor, int size) {
    a.requireLogin();
    return views.page(
        pages.fetch(
            "practice-history:" + id,
            a.id(),
            cursor,
            size,
            (b, s) -> collections.history(a, id, b, s)),
        HistoryVO.class);
  }

  public PageVO<FeedbackVO> feedback(Actor a, long note, String cursor, int size) {
    content.requirePublic(note, false);
    return views.page(
        pages.fetch(
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
                    .toList()),
        FeedbackVO.class);
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
}
