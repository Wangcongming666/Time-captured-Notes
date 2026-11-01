package cn.shiguang.notes.modules.collection.infrastructure;

import cn.shiguang.notes.modules.collection.domain.CollectionRepository;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.Data;
import cn.shiguang.notes.shared.error.BizException;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlCollectionRepository implements CollectionRepository {
  private final Database db;

  public MySqlCollectionRepository(Database db) {
    this.db = db;
  }

  public Map<String, Object> folder(long id, boolean lock) {
    return db.one("SELECT * FROM t_folder WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public List<Map<String, Object>> folders(long owner, long before, int size) {
    return db.query(
        "SELECT id,name,version FROM t_folder WHERE owner_id=? AND id<? ORDER BY id DESC LIMIT ?",
        owner,
        before,
        size);
  }

  public long createFolder(long owner, String name) {
    return db.insert("t_folder", Data.map("owner_id", owner, "name", name));
  }

  public void renameFolder(long id, String name, int version) {
    if (db.update(
            "UPDATE t_folder SET name=?,version=version+1 WHERE id=? AND version=?",
            name,
            id,
            version)
        != 1) throw BizException.conflict("收藏夹已变化");
  }

  public void deleteFolder(long id) {
    db.update("DELETE FROM t_favorite WHERE folder_id=?", id);
    db.update("DELETE FROM t_folder WHERE id=?", id);
  }

  public boolean favorite(long user, long folder, long note, boolean desired) {
    return desired
        ? db.update(
                "INSERT IGNORE INTO t_favorite(user_id,folder_id,note_id) VALUES(?,?,?)",
                user,
                folder,
                note)
            > 0
        : db.update("DELETE FROM t_favorite WHERE folder_id=? AND note_id=?", folder, note) > 0;
  }

  public boolean hasFavorite(long user, long note) {
    return db.count("SELECT COUNT(*) FROM t_favorite WHERE user_id=? AND note_id=?", user, note)
        > 0;
  }

  public List<Map<String, Object>> favorites(long folder, long before, int size) {
    return db.query(
        "SELECT id,note_id FROM t_favorite WHERE folder_id=? AND id<? ORDER BY id DESC LIMIT ?",
        folder,
        before,
        size);
  }

  public long createPractice(long owner, long note) {
    long id = db.insert("t_practice", Data.map("owner_id", owner, "note_id", note));
    db.insert(
        "t_practice_history",
        Data.map(
            "practice_id", id, "from_status", null, "to_status", "PLANNED", "actor_id", owner));
    return id;
  }

  public Map<String, Object> practice(long id, boolean lock) {
    var p = db.one("SELECT * FROM t_practice WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
    p.put(
        "mediaIds",
        db
            .query(
                "SELECT media_id FROM t_practice_media WHERE practice_id=? ORDER BY position", id)
            .stream()
            .map(r -> Data.text(r, "mediaId"))
            .toList());
    return p;
  }

  public List<Map<String, Object>> practices(long owner, long before, int size) {
    return db.query(
        "SELECT * FROM t_practice WHERE owner_id=? AND id<? ORDER BY id DESC LIMIT ?",
        owner,
        before,
        size);
  }

  public void transition(long id, long actor, String from, String to) {
    db.update(
        "UPDATE t_practice SET"
            + " status=?,version=version+1,started_at=IF(?='DOING',UTC_TIMESTAMP(3),started_at),finished_at=IF(?"
            + " IN ('DONE','ABANDONED'),UTC_TIMESTAMP(3),finished_at) WHERE id=?",
        to,
        to,
        to,
        id);
    db.insert(
        "t_practice_history",
        Data.map("practice_id", id, "from_status", from, "to_status", to, "actor_id", actor));
  }

  public void result(long id, String text, String helpfulness) {
    db.update(
        "UPDATE t_practice SET"
            + " result_text=?,helpfulness=?,public_status='PRIVATE',feedback_revision=feedback_revision+1,version=version+1"
            + " WHERE id=?",
        text,
        helpfulness,
        id);
  }

  public void publicState(long id, String state, Integer source, boolean newRevision) {
    db.update(
        "UPDATE t_practice SET"
            + " public_status=?,source_revision=COALESCE(?,source_revision),feedback_revision=feedback_revision+?,version=version+1"
            + " WHERE id=?",
        state,
        source,
        newRevision ? 1 : 0,
        id);
  }

  public List<Map<String, Object>> history(long id, long before, int size) {
    return db.query(
        "SELECT id,from_status,to_status,created_at FROM t_practice_history WHERE practice_id=? AND"
            + " id<? ORDER BY id DESC LIMIT ?",
        id,
        before,
        size);
  }
}
