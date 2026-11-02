package cn.shiguang.notes.service.impl;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.repository.ContentRepository;
import cn.shiguang.notes.service.ContentService;
import cn.shiguang.notes.validation.NoteRules;
import java.util.*;
import org.springframework.stereotype.Service;

/** Content aggregate policy. Review creation is coordinated by CommunityService. */
@Service
public class ContentServiceImpl implements ContentService {
  private final ContentRepository repo;

  public ContentServiceImpl(ContentRepository repo) {
    this.repo = repo;
  }

  public Map<String, Object> note(long id, boolean lock) {
    return repo.note(id, lock);
  }

  public Map<String, Object> requirePublic(long id, boolean lock) {
    var n = note(id, lock);
    if (!NoteRules.isPublic(Data.text(n, "status"))) throw BizException.missing();
    return n;
  }

  private Map<String, Object> own(Actor a, long id) {
    a.requireLogin();
    var n = note(id, true);
    if (Data.id(n, "authorId") != a.id()) throw BizException.missing();
    return n;
  }

  private void version(Map<String, Object> row, int version) {
    if (Data.number(row, "version") != version) throw BizException.conflict("版本已更新，请刷新后重试");
  }

  public Map<String, Object> detail(long id, Actor actor, boolean owner) {
    var n = owner ? repo.note(id, false) : requirePublic(id, false);
    if (owner && Data.id(n, "authorId") != actor.id()) throw BizException.missing();
    var out = Data.select(n, "id", "authorId", "title", "body", "publishedAt");
    out.put("mediaIds", repo.mediaIds(id).stream().map(Object::toString).toList());
    out.put("mediaEntries", repo.mediaEntries(id));
    out.put("topicIds", repo.topicIds(id).stream().map(Object::toString).toList());
    if (owner) {
      out.put("status", n.get("status"));
      out.put("version", n.get("version"));
      out.put("revision", n.get("revision"));
      out.put("reason", n.get("rejectReason"));
    }
    return out;
  }

  public List<Map<String, Object>> owned(long actor, long before, int size) {
    return repo.owned(actor, before, size);
  }

  public Map<String, Object> preview(long id) {
    var n = repo.note(id, false);
    var out =
        Data.select(
            n, "id", "authorId", "title", "body", "status", "version", "revision", "publishedAt");
    out.put("reason", n.get("rejectReason"));
    out.put("mediaIds", repo.mediaIds(id).stream().map(Object::toString).toList());
    out.put("mediaEntries", repo.mediaEntries(id));
    out.put("topicIds", repo.topicIds(id).stream().map(Object::toString).toList());
    return out;
  }

  public Map<String, Object> save(Actor actor, Long id, Map<String, Object> body) {
    actor.requireLogin();
    String title = Data.text(body, "title"), text = Data.text(body, "body");
    var images = Data.ids(body, "mediaIds");
    var topics = Data.ids(body, "topicIds");
    NoteRules.length(title, 0, 40, "标题");
    NoteRules.length(text, 0, 2000, "正文");
    if (images.size() > 9 || topics.size() > 5) throw BizException.invalid("图片或话题数量超限");
    if (!repo.activeTopics(topics)) throw BizException.invalid("存在停用话题");
    if (id == null) {
      id = repo.create(actor.id(), title, text);
    } else {
      var n = own(actor, id);
      version(n, Data.number(body, "version"));
      String status = NoteRules.afterEdit(Data.text(n, "status"));
      if (status.equals("PENDING"))
        NoteRules.validateSubmission(title, text, images.size(), topics.size());
      repo.edit(id, title, text, status, Data.number(body, "version"));
    }
    repo.bind(actor.id(), images, "NOTE", "NOTE", id);
    repo.topics(id, topics);
    return note(id, false);
  }

  public Map<String, Object> submit(Actor actor, long id, int version) {
    var n = own(actor, id);
    version(n, version);
    if (!"DRAFT".equals(n.get("status"))) throw BizException.conflict("只有草稿可以提交审核");
    var topics = repo.topicIds(id);
    if (!repo.activeTopics(topics)) throw BizException.invalid("存在停用话题");
    NoteRules.validateSubmission(
        Data.text(n, "title"), Data.text(n, "body"), repo.mediaIds(id).size(), topics.size());
    repo.state(id, "PENDING", null, true);
    return note(id, false);
  }

  public Map<String, Object> withdraw(Actor actor, long id, int version) {
    var n = own(actor, id);
    version(n, version);
    if (!"PENDING".equals(n.get("status"))) throw BizException.conflict("当前笔记不在审核中");
    repo.state(id, "DRAFT", null, false);
    return note(id, false);
  }

  public void delete(Actor actor, long id, int version) {
    var n = own(actor, id);
    version(n, version);
    if (!"DELETED".equals(n.get("status"))) repo.state(id, "DELETED", null, false);
  }

  public void moderate(long id, int revision, String decision, String reason) {
    var n = note(id, true);
    if (!"PENDING".equals(n.get("status")) || Data.number(n, "revision") != revision)
      throw BizException.conflict("审核任务已过期");
    repo.state(id, decision.equals("APPROVE") ? "PUBLISHED" : "REJECTED", reason, false);
  }

  public void offline(long id, int version, String reason) {
    var n = requirePublic(id, true);
    version(n, version);
    repo.state(id, "OFFLINE", reason, false);
  }

  public Map<String, Object> addMedia(
      Actor actor,
      String key,
      String purpose,
      String mime,
      long size,
      int width,
      int height,
      String sha) {
    actor.requireLogin();
    long id =
        repo.addMedia(
            Data.map(
                "owner_id",
                actor.id(),
                "storage_key",
                key,
                "purpose",
                purpose,
                "mime",
                mime,
                "size_bytes",
                size,
                "width",
                width,
                "height",
                height,
                "sha256",
                sha));
    return Data.map(
        "id", Long.toString(id), "mime", mime, "sizeBytes", size, "width", width, "height", height);
  }

  public Map<String, Object> addVideo(Actor actor, Map<String, Object> metadata) {
    actor.requireLogin();
    var values = new LinkedHashMap<>(metadata);
    values.put("owner_id", actor.id());
    long id = repo.addMedia(values);
    return Data.map(
        "id",
        Long.toString(id),
        "mediaType",
        "VIDEO",
        "mime",
        "video/mp4",
        "width",
        values.get("width"),
        "height",
        values.get("height"),
        "durationMs",
        values.get("duration_ms"));
  }

  public Map<String, Object> media(long id) {
    return repo.media(id, false);
  }

  public void bind(Actor actor, List<Long> images, String purpose, String type, long target) {
    repo.bind(actor.id(), images, purpose, type, target);
  }

  public void avatar(Actor actor, Long next, Long previous) {
    repo.avatar(actor.id(), next, previous);
  }
}
