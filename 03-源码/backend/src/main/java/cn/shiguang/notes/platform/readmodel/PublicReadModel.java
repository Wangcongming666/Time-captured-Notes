package cn.shiguang.notes.platform.readmodel;

import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.*;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Read-only cross-context projections; no cached authorization. */
@Repository
public class PublicReadModel {
  private final Database db;

  public PublicReadModel(Database db) {
    this.db = db;
  }

  public Map<String, Object> profileSummary(long user, long viewer) {
    var row =
        db.one(
            "SELECT (SELECT COUNT(*) FROM t_follow WHERE follower_id=?) AS following_count,(SELECT"
                + " COUNT(*) FROM t_follow WHERE followee_id=?) AS follower_count,(SELECT COUNT(*)"
                + " FROM t_note WHERE author_id=? AND status='PUBLISHED') AS note_count,(SELECT"
                + " COALESCE(SUM(m.like_count+m.favorite_count),0) FROM t_note n JOIN t_note_metric"
                + " m ON m.note_id=n.id WHERE n.author_id=? AND n.status='PUBLISHED') AS"
                + " appreciation_count,EXISTS(SELECT 1 FROM t_follow WHERE follower_id=? AND"
                + " followee_id=?) AS following",
            user,
            user,
            user,
            user,
            viewer,
            user);
    Object following = row.get("following");
    row.put(
        "following",
        Boolean.TRUE.equals(following)
            || following instanceof Number number && number.intValue() != 0);
    return row;
  }

  public boolean noteVisible(long id) {
    return db.count("SELECT COUNT(*) FROM t_note WHERE id=? AND status='PUBLISHED'", id) == 1;
  }

  public boolean practiceVisible(long id) {
    return db.count(
            "SELECT COUNT(*) FROM t_practice p JOIN t_note n ON n.id=p.note_id WHERE p.id=? AND"
                + " p.public_status='PUBLISHED' AND n.status='PUBLISHED' AND"
                + " p.source_revision=n.revision",
            id)
        == 1;
  }

  public boolean commentVisible(long id) {
    return db.count(
            "SELECT COUNT(*) FROM t_comment c JOIN t_note n ON n.id=c.note_id WHERE c.id=? AND"
                + " c.status='VISIBLE' AND n.status='PUBLISHED'",
            id)
        == 1;
  }

  public boolean targetVisible(String type, long id) {
    return switch (type) {
      case "NOTE" -> noteVisible(id);
      case "COMMENT" -> commentVisible(id);
      case "PRACTICE" -> practiceVisible(id);
      case "USER" ->
          db.count("SELECT COUNT(*) FROM t_user WHERE id=? AND status='ACTIVE'", id) == 1;
      default -> false;
    };
  }

  public boolean mediaAllowed(Actor actor, Map<String, Object> media) {
    long id = Data.id(media, "id");
    if ("DELETED".equals(media.get("status"))) return false;
    if (actor.id() > 0 && Data.id(media, "ownerId") == actor.id()) return true;
    if (db.count(
            "SELECT COUNT(*) FROM t_note_media nm JOIN t_note n ON n.id=nm.note_id WHERE"
                + " nm.media_id=? AND n.status='PUBLISHED'",
            id)
        > 0) return true;
    if (db.count(
            "SELECT COUNT(*) FROM t_practice_media pm JOIN t_practice p ON p.id=pm.practice_id JOIN"
                + " t_note n ON n.id=p.note_id WHERE pm.media_id=? AND p.public_status='PUBLISHED'"
                + " AND n.status='PUBLISHED' AND p.source_revision=n.revision",
            id)
        > 0) return true;
    if (db.count("SELECT COUNT(*) FROM t_user WHERE avatar_media_id=? AND status='ACTIVE'", id) > 0)
      return true;
    return actor.has("REVIEWER")
        && db.count(
                "SELECT COUNT(*) FROM t_review r WHERE r.status='PENDING' AND r.applicant_id<>? AND"
                    + " ((r.target_type='NOTE' AND EXISTS(SELECT 1 FROM t_note n JOIN t_note_media"
                    + " nm ON nm.note_id=n.id WHERE n.id=r.target_id AND"
                    + " n.revision=r.target_revision AND nm.media_id=?)) OR"
                    + " (r.target_type='PRACTICE' AND EXISTS(SELECT 1 FROM t_practice p JOIN"
                    + " t_practice_media pm ON pm.practice_id=p.id WHERE p.id=r.target_id AND"
                    + " p.feedback_revision=r.target_revision AND pm.media_id=?)))",
                actor.id(),
                id,
                id)
            > 0;
  }

  public Map<String, Object> interaction(long user, long note) {
    var r =
        db.one(
            "SELECT (SELECT COUNT(*) FROM t_like WHERE user_id=? AND note_id=?) AS liked,(SELECT"
                + " COUNT(*) FROM t_favorite WHERE user_id=? AND note_id=?) AS favorite,(SELECT"
                + " COUNT(*) FROM t_follow f JOIN t_note n ON n.author_id=f.followee_id WHERE"
                + " f.follower_id=? AND n.id=?) AS following,COALESCE((SELECT like_count FROM"
                + " t_note_metric WHERE note_id=?),0) AS like_count,COALESCE((SELECT favorite_count"
                + " FROM t_note_metric WHERE note_id=?),0) AS favorite_count,COALESCE((SELECT"
                + " comment_count FROM t_note_metric WHERE note_id=?),0) AS comment_count",
            user,
            note,
            user,
            note,
            user,
            note,
            note,
            note,
            note);
    for (String field : List.of("liked", "favorite", "following"))
      r.put(field, ((Number) r.get(field)).longValue() > 0);
    return r;
  }

  public List<Map<String, Object>> feedback(long note, long before, int size) {
    return db.query(
        "SELECT p.id,p.note_id,p.owner_id AS author_id,p.result_text,p.helpfulness FROM t_practice"
            + " p JOIN t_note n ON n.id=p.note_id WHERE p.note_id=? AND p.id<? AND"
            + " p.public_status='PUBLISHED' AND n.status='PUBLISHED' AND"
            + " p.source_revision=n.revision ORDER BY p.id DESC LIMIT ?",
        note,
        before,
        size);
  }

  public List<Map<String, Object>> users(long before, int size) {
    var rows =
        db.query(
            "SELECT u.id,u.username,u.nickname,u.status,GROUP_CONCAT(r.code ORDER BY r.code) AS"
                + " role_codes FROM t_user u LEFT JOIN t_user_role ur ON ur.user_id=u.id LEFT JOIN"
                + " t_role r ON r.id=ur.role_id WHERE u.id<? GROUP BY u.id ORDER BY u.id DESC LIMIT"
                + " ?",
            before,
            size);
    for (var row : rows) {
      String codes = Data.text(row, "roleCodes");
      row.remove("roleCodes");
      row.put("roles", codes.isBlank() ? List.of() : List.of(codes.split(",")));
    }
    return rows;
  }
}
