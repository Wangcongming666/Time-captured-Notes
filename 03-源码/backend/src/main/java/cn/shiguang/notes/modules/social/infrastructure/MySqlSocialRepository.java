package cn.shiguang.notes.modules.social.infrastructure;

import cn.shiguang.notes.modules.social.domain.SocialRepository;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.Data;
import cn.shiguang.notes.shared.error.BizException;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlSocialRepository implements SocialRepository {
  private final Database db;

  public MySqlSocialRepository(Database db) {
    this.db = db;
  }

  public boolean like(long user, long note, boolean desired) {
    return desired
        ? db.update("INSERT IGNORE INTO t_like(user_id,note_id) VALUES(?,?)", user, note) > 0
        : db.update("DELETE FROM t_like WHERE user_id=? AND note_id=?", user, note) > 0;
  }

  public boolean follow(long from, long to, boolean desired) {
    return desired
        ? db.update("INSERT IGNORE INTO t_follow(follower_id,followee_id) VALUES(?,?)", from, to)
            > 0
        : db.update("DELETE FROM t_follow WHERE follower_id=? AND followee_id=?", from, to) > 0;
  }

  public Map<String, Object> comment(long id, boolean lock) {
    return db.one("SELECT * FROM t_comment WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public long createComment(long author, long note, Long root, Long reply, String body) {
    return db.insert(
        "t_comment",
        Data.map(
            "author_id",
            author,
            "note_id",
            note,
            "root_id",
            root,
            "reply_to_id",
            reply,
            "body",
            body));
  }

  public void state(long id, String state, int version) {
    if (db.update(
            "UPDATE t_comment SET status=?,body=IF(?='DELETED','',body),version=version+1 WHERE"
                + " id=? AND version=?",
            state,
            state,
            id,
            version)
        != 1) throw BizException.conflict("评论已变化");
  }

  public List<Map<String, Object>> comments(long note, Long root, long before, int size) {
    if (root == null)
      return db.query(
          "SELECT id,author_id,note_id,root_id,reply_to_id,IF(status='VISIBLE',body,'该评论已不可见') AS"
              + " body,status,created_at FROM t_comment WHERE note_id=? AND root_id IS NULL AND"
              + " id<? ORDER BY id DESC LIMIT ?",
          note,
          before,
          size);
    return db.query(
        "SELECT id,author_id,note_id,root_id,reply_to_id,IF(status='VISIBLE',body,'该回复已不可见') AS"
            + " body,status,created_at FROM t_comment WHERE note_id=? AND root_id=? AND id<? ORDER"
            + " BY id DESC LIMIT ?",
        note,
        root,
        before,
        size);
  }

  public List<Map<String, Object>> follows(long user, boolean followers, long before, int size) {
    String relation = followers ? "follower_id" : "followee_id",
        filter = followers ? "followee_id" : "follower_id";
    return db.query(
        "SELECT f.id AS relation_id,u.id,u.nickname,u.bio,u.avatar_media_id FROM t_follow f JOIN"
            + " t_user u ON u.id=f."
            + relation
            + " WHERE f."
            + filter
            + "=? AND f.id<? AND u.status='ACTIVE' ORDER BY f.id DESC LIMIT ?",
        user,
        before,
        size);
  }
}
