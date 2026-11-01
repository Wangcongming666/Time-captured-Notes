package cn.shiguang.notes.modules.collection.application;

import cn.shiguang.notes.modules.collection.api.CollectionApi;
import cn.shiguang.notes.modules.collection.domain.*;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class CollectionService implements CollectionApi {
  private final CollectionRepository repo;

  public CollectionService(CollectionRepository repo) {
    this.repo = repo;
  }

  private void name(String n) {
    if (n.isBlank() || n.codePointCount(0, n.length()) > 30)
      throw BizException.invalid("收藏夹名称须为1–30字");
  }

  public Map<String, Object> folder(Actor actor, long id, boolean lock) {
    var f = repo.folder(id, lock);
    if (Data.id(f, "ownerId") != actor.id()) throw BizException.missing();
    return f;
  }

  public List<Map<String, Object>> folders(Actor actor, long before, int size) {
    return repo.folders(actor.id(), before, size);
  }

  public long createFolder(Actor actor, String n) {
    name(n);
    return repo.createFolder(actor.id(), n);
  }

  public Map<String, Object> renameFolder(Actor actor, long id, String n, int version) {
    folder(actor, id, true);
    name(n);
    repo.renameFolder(id, n, version);
    return Data.select(repo.folder(id, false), "id", "name", "version");
  }

  public void deleteFolder(Actor actor, long id) {
    folder(actor, id, true);
    repo.deleteFolder(id);
  }

  public boolean favorite(Actor actor, long folder, long note, boolean desired) {
    folder(actor, folder, true);
    return repo.favorite(actor.id(), folder, note, desired);
  }

  public boolean hasFavorite(long user, long note) {
    return repo.hasFavorite(user, note);
  }

  public List<Map<String, Object>> favorites(Actor actor, long folder, long before, int size) {
    folder(actor, folder, false);
    return repo.favorites(folder, before, size);
  }

  public long createPractice(Actor actor, long note) {
    if (!repo.hasFavorite(actor.id(), note)) throw BizException.conflict("请先收藏这篇笔记");
    return repo.createPractice(actor.id(), note);
  }

  public Map<String, Object> practice(long id, boolean lock) {
    return repo.practice(id, lock);
  }

  public Map<String, Object> ownPractice(Actor actor, long id, boolean lock) {
    var p = repo.practice(id, lock);
    if (Data.id(p, "ownerId") != actor.id()) throw BizException.missing();
    return p;
  }

  public List<Map<String, Object>> practices(Actor actor, long before, int size) {
    return repo.practices(actor.id(), before, size);
  }

  private void version(Map<String, Object> p, int expected) {
    if (Data.number(p, "version") != expected) throw BizException.conflict("实践已变化，请刷新");
  }

  public Map<String, Object> transition(Actor actor, long id, int v, String to) {
    var p = ownPractice(actor, id, true);
    version(p, v);
    String from = Data.text(p, "status");
    PracticeRules.transition(from, to);
    repo.transition(id, actor.id(), from, to);
    return repo.practice(id, false);
  }

  public Map<String, Object> result(Actor actor, long id, Map<String, Object> input) {
    var p = ownPractice(actor, id, true);
    version(p, Data.number(input, "version"));
    if (!"DONE".equals(p.get("status"))) throw BizException.conflict("完成实践后才能填写结果");
    String text = Data.text(input, "resultText"), help = Data.text(input, "helpfulness");
    if (text.isBlank()
        || text.codePointCount(0, text.length()) > 2000
        || !Set.of("HELPFUL", "NEUTRAL", "NOT_APPLICABLE").contains(help))
      throw BizException.invalid("请填写有效结果及体验评价");
    repo.result(id, text, help);
    return repo.practice(id, false);
  }

  public Map<String, Object> publishRequest(Actor actor, long id, int v, int source) {
    var p = ownPractice(actor, id, true);
    version(p, v);
    if (!"DONE".equals(p.get("status"))
        || Data.text(p, "resultText").isBlank()
        || "PENDING".equals(p.get("publicStatus"))
        || "PUBLISHED".equals(p.get("publicStatus"))) throw BizException.conflict("当前结果不能申请公开");
    repo.publicState(id, "PENDING", source, true);
    return repo.practice(id, false);
  }

  public Map<String, Object> unpublish(Actor actor, long id, int v) {
    var p = ownPractice(actor, id, true);
    version(p, v);
    repo.publicState(id, "PRIVATE", null, false);
    return repo.practice(id, false);
  }

  public void moderate(long id, int revision, String decision) {
    var p = repo.practice(id, true);
    if (!"PENDING".equals(p.get("publicStatus")) || Data.number(p, "feedbackRevision") != revision)
      throw BizException.conflict("反馈审核已过期");
    repo.publicState(id, decision.equals("APPROVE") ? "PUBLISHED" : "REJECTED", null, false);
  }

  public void offline(long id, int version) {
    var p = repo.practice(id, true);
    version(p, version);
    if (!"PUBLISHED".equals(p.get("publicStatus"))) throw BizException.conflict("反馈尚未公开");
    repo.publicState(id, "OFFLINE", null, false);
  }

  public List<Map<String, Object>> history(Actor actor, long id, long before, int size) {
    ownPractice(actor, id, false);
    return repo.history(id, before, size);
  }
}
