package cn.shiguang.notes.service.application;

import cn.shiguang.notes.common.contract.EventPort;
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

/** Content use cases: authorization, transaction orchestration, and typed public projections. */
@Service
public class ContentApplicationService {
  private final ContentService content;
  private final GovernanceService governance;
  private final StatisticsService statistics;
  private final EventPort events;
  private final Commands commands;
  private final Pages pages;
  private final PublicQueryRepository read;
  private final HttpBoundaryConverter views;

  public ContentApplicationService(
      ContentService content,
      GovernanceService governance,
      StatisticsService statistics,
      EventPort events,
      Commands commands,
      Pages pages,
      PublicQueryRepository read,
      HttpBoundaryConverter views) {
    this.content = content;
    this.governance = governance;
    this.statistics = statistics;
    this.events = events;
    this.commands = commands;
    this.pages = pages;
    this.read = read;
    this.views = views;
  }

  @Transactional
  public VersionVO createNote(Actor actor, CreateNoteRequest request, String key) {
    return saveNoteInput(actor, null, views.input(request), key);
  }

  @Transactional
  public VersionVO editNote(Actor actor, long id, EditNoteRequest request, String key) {
    return saveNoteInput(actor, id, views.input(request), key);
  }

  private VersionVO saveNoteInput(Actor actor, Long id, Map<String, Object> body, String key) {
    return views.view(
        commands.run(
            actor,
            "note:save:" + id,
            key,
            body,
            () -> {
              var row = content.save(actor, id, body);
              long target = Data.id(row, "id");
              governance.cancelReviews("NOTE", target);
              if ("PENDING".equals(row.get("status")))
                governance.openReview("NOTE", target, Data.number(row, "revision"), actor.id());
              changed(actor, "NoteVisibilityChanged", "NOTE", target, notePayload(row));
              return version(row);
            }),
        VersionVO.class);
  }

  @Transactional
  public VersionVO submit(Actor actor, long id, VersionRequest request, String key) {
    int v = request.version();
    return views.view(
        commands.run(
            actor,
            "note:submit:" + id,
            key,
            Data.map("version", v),
            () -> {
              var n = content.submit(actor, id, v);
              governance.openReview("NOTE", id, Data.number(n, "revision"), actor.id());
              changed(actor, "NoteSubmitted", "NOTE", id, notePayload(n));
              return version(n);
            }),
        VersionVO.class);
  }

  @Transactional
  public VersionVO withdraw(Actor actor, long id, VersionRequest request, String key) {
    int v = request.version();
    return views.view(
        commands.run(
            actor,
            "note:withdraw:" + id,
            key,
            Data.map("version", v),
            () -> {
              var n = content.withdraw(actor, id, v);
              governance.cancelReviews("NOTE", id);
              changed(actor, "NoteVisibilityChanged", "NOTE", id, notePayload(n));
              return version(n);
            }),
        VersionVO.class);
  }

  @Transactional
  public void deleteNote(Actor actor, long id, int v) {
    actor.requireLogin();
    var n = content.note(id, true);
    content.delete(actor, id, v);
    governance.cancelReviews("NOTE", id);
    changed(actor, "NoteVisibilityChanged", "NOTE", id, notePayload(n));
  }

  @Transactional
  public NoteVO note(Actor actor, long id, String subject) {
    var n = content.detail(id, actor, false);
    n.putAll(read.interaction(actor.id(), id));
    statistics.viewed(actor.id(), subject, id, Data.id(n, "authorId"));
    return views.view(n, NoteVO.class);
  }

  public OwnedNoteVO ownNote(Actor actor, long id) {
    actor.requireLogin();
    return views.view(content.detail(id, actor, true), OwnedNoteVO.class);
  }

  public PageVO<OwnedNoteVO> ownNotes(Actor a, String cursor, int size) {
    a.requireLogin();
    return views.page(
        pages.fetch(
            "own-notes",
            a.id(),
            cursor,
            size,
            (b, s) ->
                content.owned(a.id(), b, s).stream()
                    .map(n -> content.preview(Data.id(n, "id")))
                    .toList()),
        OwnedNoteVO.class);
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
}
