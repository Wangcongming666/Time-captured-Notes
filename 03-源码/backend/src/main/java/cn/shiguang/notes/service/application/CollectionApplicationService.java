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

/** Collection use cases: authorization, transaction orchestration, and typed public projections. */
@Service
public class CollectionApplicationService {
  private final ContentService content;
  private final CollectionService collections;
  private final EventPort events;
  private final Commands commands;
  private final Pages pages;
  private final PublicQueryRepository read;
  private final HttpBoundaryConverter views;

  public CollectionApplicationService(
      ContentService content,
      CollectionService collections,
      EventPort events,
      Commands commands,
      Pages pages,
      PublicQueryRepository read,
      HttpBoundaryConverter views) {
    this.content = content;
    this.collections = collections;
    this.events = events;
    this.commands = commands;
    this.pages = pages;
    this.read = read;
    this.views = views;
  }

  @Transactional
  public IdVO createFolder(Actor actor, CreateFolderRequest request, String key) {
    var body = views.input(request);
    return views.view(
        commands.run(
            actor,
            "folder:create",
            key,
            body,
            () ->
                Map.of(
                    "id", Long.toString(collections.createFolder(actor, Data.text(body, "name"))))),
        IdVO.class);
  }

  @Transactional
  public FolderVO renameFolder(Actor actor, long id, RenameFolderRequest request) {
    var body = views.input(request);
    actor.requireLogin();
    return views.view(
        collections.renameFolder(actor, id, Data.text(body, "name"), Data.number(body, "version")),
        FolderVO.class);
  }

  @Transactional
  public void deleteFolder(Actor actor, long id) {
    actor.requireLogin();
    collections.deleteFolder(actor, id);
    events.append(
        "FavoriteChanged", actor.id(), "USER", actor.id(), 0, Map.of(), "statistics", "cache");
  }

  @Transactional
  public void favorite(Actor actor, long folder, long note, boolean desired) {
    actor.requireLogin();
    var n = desired ? content.requirePublic(note, true) : content.note(note, true);
    if (collections.favorite(actor, folder, note, desired)) {
      var payload = notePayload(n);
      payload.put("desired", desired);
      changed(actor, "FavoriteChanged", "NOTE", note, payload);
    }
  }

  public PageVO<FolderVO> folders(Actor a, String cursor, int size) {
    a.requireLogin();
    return views.page(
        pages.fetch("folders", a.id(), cursor, size, (b, s) -> collections.folders(a, b, s)),
        FolderVO.class);
  }

  @SuppressWarnings("unchecked")
  public PageVO<FavoriteVO> favorites(Actor a, long folder, String cursor, int size) {
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
    return views.page(result, FavoriteVO.class);
  }

  private void changed(
      Actor actor, String type, String aggregate, long id, Map<String, Object> payload) {
    events.append(type, actor.id(), aggregate, id, 0, payload, "statistics", "cache");
  }

  private Map<String, Object> notePayload(Map<String, Object> note) {
    return Data.map("noteId", note.get("id"), "authorId", note.get("authorId"));
  }
}
