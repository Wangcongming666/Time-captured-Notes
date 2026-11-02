package cn.shiguang.notes.modules.discovery.infrastructure;

import cn.shiguang.notes.modules.discovery.domain.DiscoveryRepository;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.Data;
import cn.shiguang.notes.shared.error.BizException;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlDiscoveryRepository implements DiscoveryRepository {
  private final Database db;

  public MySqlDiscoveryRepository(Database db) {
    this.db = db;
  }

  public List<Map<String, Object>> candidates(
      long user, String mode, String category, String keyword, Long topic, Long author) {
    String sql =
        "SELECT n.id,n.author_id,n.title,n.published_at,(SELECT media_id FROM t_note_media nm WHERE"
            + " nm.note_id=n.id ORDER BY position LIMIT 1) AS cover_media_id,cover.media_type AS"
            + " cover_media_type,cover.duration_ms, u.avatar_media_id AS"
            + " author_avatar_media_id,u.nickname AS author_name,COALESCE(m.like_count,0) AS"
            + " like_count,COALESCE(m.favorite_count,0) AS"
            + " favorite_count,COALESCE(m.comment_count,0) AS comment_count,EXISTS(SELECT 1 FROM"
            + " t_follow f WHERE f.follower_id=? AND f.followee_id=n.author_id) AS"
            + " following,EXISTS(SELECT 1 FROM t_note_topic nt JOIN t_interest i ON"
            + " i.topic_id=nt.topic_id WHERE nt.note_id=n.id AND i.user_id=? AND i.mode='LIKE') AS"
            + " interest,EXISTS(SELECT 1 FROM t_note_topic nt JOIN t_interest i ON"
            + " i.topic_id=nt.topic_id WHERE nt.note_id=n.id AND i.user_id=? AND i.mode='REDUCE')"
            + " AS reduced FROM t_note n JOIN t_user u ON u.id=n.author_id LEFT JOIN t_note_metric"
            + " m ON m.note_id=n.id LEFT JOIN t_note_media first_media ON first_media.note_id=n.id"
            + " AND first_media.position=1 LEFT JOIN t_media cover ON cover.id=first_media.media_id"
            + " WHERE n.status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM t_hidden_note h WHERE"
            + " h.note_id=n.id AND h.user_id=?)";
    var args = new ArrayList<Object>(List.of(user, user, user, user));
    if (mode.equals("following")) {
      sql +=
          " AND EXISTS(SELECT 1 FROM t_follow f WHERE f.follower_id=? AND"
              + " f.followee_id=n.author_id)";
      args.add(user);
    }
    if (category != null && !category.isBlank() && !category.equals("全部")) {
      sql +=
          " AND EXISTS(SELECT 1 FROM t_note_topic nt JOIN t_topic t ON t.id=nt.topic_id WHERE"
              + " nt.note_id=n.id AND t.category=?)";
      args.add(category);
    }
    if (keyword != null && !keyword.isBlank()) {
      if (keyword.length() > 100) throw BizException.invalid("关键词过长");
      sql +=
          " AND (n.title LIKE ? ESCAPE '!' OR n.body LIKE ? ESCAPE '!' OR u.nickname LIKE ? ESCAPE"
              + " '!')";
      String q = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
      args.add(q);
      args.add(q);
      args.add(q);
    }
    if (topic != null) {
      sql += " AND EXISTS(SELECT 1 FROM t_note_topic nt WHERE nt.note_id=n.id AND nt.topic_id=?)";
      args.add(topic);
    }
    if (author != null) {
      sql += " AND n.author_id=?";
      args.add(author);
    }
    return db.query(sql + " ORDER BY n.published_at DESC,n.id DESC LIMIT 300", args.toArray());
  }

  public Set<String> visible(long user, List<String> ids) {
    if (ids.isEmpty()) return Set.of();
    var args = new ArrayList<Object>();
    args.add(user);
    args.addAll(ids);
    String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
    return new HashSet<>(
        db
            .query(
                "SELECT n.id FROM t_note n WHERE n.status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM"
                    + " t_hidden_note h WHERE h.user_id=? AND h.note_id=n.id) AND n.id IN ("
                    + marks
                    + ")",
                args.toArray())
            .stream()
            .map(r -> Data.text(r, "id"))
            .toList());
  }

  public Map<String, Object> preferences(long user) {
    var rows = db.query("SELECT topic_id,mode FROM t_interest WHERE user_id=?", user);
    return Data.map(
        "interests",
        rows.stream().filter(r -> r.get("mode").equals("LIKE")).map(r -> r.get("topicId")).toList(),
        "reducedTopics",
        rows.stream()
            .filter(r -> r.get("mode").equals("REDUCE"))
            .map(r -> r.get("topicId"))
            .toList());
  }

  public void preferences(long user, List<Long> likes, List<Long> reduced) {
    db.update("DELETE FROM t_interest WHERE user_id=?", user);
    for (long t : likes) interest(user, t, "LIKE");
    for (long t : reduced) interest(user, t, "REDUCE");
  }

  private void interest(long user, long topic, String mode) {
    if (db.count("SELECT COUNT(*) FROM t_topic WHERE id=? AND status='ACTIVE'", topic) != 1)
      throw BizException.invalid("无效话题");
    db.update("INSERT INTO t_interest(user_id,topic_id,mode) VALUES(?,?,?)", user, topic, mode);
  }

  public void hide(long user, long note, boolean hidden) {
    if (hidden)
      db.update("INSERT IGNORE INTO t_hidden_note(user_id,note_id) VALUES(?,?)", user, note);
    else db.update("DELETE FROM t_hidden_note WHERE user_id=? AND note_id=?", user, note);
  }

  public void reset(long user) {
    db.update("DELETE FROM t_interest WHERE user_id=?", user);
    db.update("DELETE FROM t_hidden_note WHERE user_id=?", user);
  }
}
