package cn.shiguang.notes.modules.statistics.infrastructure;

import cn.shiguang.notes.modules.statistics.domain.StatisticsRepository;
import cn.shiguang.notes.platform.persistence.Database;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlStatisticsRepository implements StatisticsRepository {
  public List<Map<String, Object>> hotTopics(int days) {
    return db.query(
        "SELECT t.id AS subject_id,t.name AS label,'LIKE' AS metric,COALESCE(SUM(m.like_count),0)"
            + " AS value,COUNT(*) AS note_count FROM t_topic t JOIN t_note_topic nt ON"
            + " nt.topic_id=t.id JOIN t_note n ON n.id=nt.note_id LEFT JOIN t_note_metric m ON"
            + " m.note_id=n.id WHERE n.status='PUBLISHED' AND"
            + " n.published_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL ? DAY) GROUP BY t.id,t.name ORDER"
            + " BY value DESC,note_count DESC,t.id DESC LIMIT 10",
        days);
  }

  private final Database db;

  public MySqlStatisticsRepository(Database db) {
    this.db = db;
  }

  public boolean view(String subject, long note) {
    return db.update(
            "INSERT IGNORE INTO t_view_window(subject_hash,note_id,window_start)"
                + " VALUES(?,?,FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP())/1800)*1800))",
            subject,
            note)
        > 0;
  }

  public void active(long user) {
    db.update(
        "INSERT IGNORE INTO t_daily_activity(day,user_id)"
            + " VALUES(DATE(DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR)),?)",
        user);
  }

  public void incrementView(long note, long author, String day) {
    metric(day, "NOTE", note, "VIEW", 1, true);
    metric(day, "AUTHOR", author, "VIEW", 1, true);
    metric(day, "PLATFORM", 0, "VIEW", 1, true);
  }

  public void metric(
      String day, String scope, long subject, String metric, double value, boolean increment) {
    db.update(
        "INSERT INTO t_daily_metric(day,scope,subject_id,metric,value,calculated_at)"
            + " VALUES(?,?,?,?,?,UTC_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE value="
            + (increment ? "value+VALUES(value)" : "VALUES(value)")
            + ",calculated_at=UTC_TIMESTAMP(3)",
        day,
        scope,
        subject,
        metric,
        value);
  }

  public void recalculate(long note) {
    db.update(
        "INSERT INTO"
            + " t_note_metric(note_id,view_count,like_count,favorite_count,comment_count,calculated_at)"
            + " SELECT ?,COALESCE((SELECT SUM(value) FROM t_daily_metric WHERE scope='NOTE' AND"
            + " subject_id=? AND metric='VIEW'),0),(SELECT COUNT(*) FROM t_like WHERE"
            + " note_id=?),(SELECT COUNT(DISTINCT user_id) FROM t_favorite WHERE note_id=?),(SELECT"
            + " COUNT(*) FROM t_comment WHERE note_id=? AND status='VISIBLE'),UTC_TIMESTAMP(3) ON"
            + " DUPLICATE KEY UPDATE"
            + " view_count=VALUES(view_count),like_count=VALUES(like_count),favorite_count=VALUES(favorite_count),comment_count=VALUES(comment_count),calculated_at=UTC_TIMESTAMP(3)",
        note,
        note,
        note,
        note,
        note);
  }

  public List<Map<String, Object>> points(String scope, long subject, int days) {
    var result =
        db.query(
            "SELECT day,metric,value FROM t_daily_metric WHERE scope=? AND subject_id=? AND"
                + " day>=DATE_SUB(DATE(DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR)),INTERVAL ? DAY)"
                + " ORDER BY day,metric",
            scope,
            subject,
            days - 1);
    if (scope.equals("PLATFORM"))
      result.addAll(
          db.query(
              "SELECT day,'DAU' AS metric,COUNT(*) AS value FROM t_daily_activity WHERE"
                  + " day>=DATE_SUB(DATE(DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR)),INTERVAL ? DAY)"
                  + " GROUP BY day",
              days - 1));
    return result;
  }

  public List<Map<String, Object>> governance(int days) {
    return db.query(
        "SELECT DATE(created_at+INTERVAL 8 HOUR) AS day,'REVIEW_PENDING' AS"
            + " metric,SUM(status='PENDING') AS value FROM t_review WHERE"
            + " created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL ? DAY) GROUP BY"
            + " DATE(created_at+INTERVAL 8 HOUR) UNION ALL SELECT DATE(created_at+INTERVAL 8"
            + " HOUR),'REVIEW_DURATION_MS',AVG(TIMESTAMPDIFF(MICROSECOND,created_at,decided_at))/1000"
            + " FROM t_review WHERE decided_at IS NOT NULL AND"
            + " created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL ? DAY) GROUP BY"
            + " DATE(created_at+INTERVAL 8 HOUR) UNION ALL SELECT DATE(created_at+INTERVAL 8"
            + " HOUR),'REPORT_ACCEPTED',COUNT(*) FROM t_report WHERE"
            + " created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL ? DAY) GROUP BY"
            + " DATE(created_at+INTERVAL 8 HOUR) UNION ALL SELECT DATE(created_at+INTERVAL 8"
            + " HOUR),'REPORT_CLOSED',SUM(status IN ('UPHELD','REJECTED')) FROM t_report WHERE"
            + " created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL ? DAY) GROUP BY"
            + " DATE(created_at+INTERVAL 8 HOUR) UNION ALL SELECT DATE(created_at+INTERVAL 8"
            + " HOUR),'PRACTICE_CREATED',COUNT(*) FROM t_practice WHERE"
            + " created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL ? DAY) GROUP BY"
            + " DATE(created_at+INTERVAL 8 HOUR) UNION ALL SELECT DATE(created_at+INTERVAL 8"
            + " HOUR),'PRACTICE_DONE',SUM(status='DONE') FROM t_practice WHERE"
            + " created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL ? DAY) GROUP BY"
            + " DATE(created_at+INTERVAL 8 HOUR)",
        days,
        days,
        days,
        days,
        days,
        days);
  }

  public List<Map<String, Object>> ranking(Long author, long before, int size) {
    long offset = before == Long.MAX_VALUE ? 0 : before;
    String base =
        "SELECT n.id AS subject_id,n.title AS label,'LIKE' AS metric,COALESCE(m.like_count,0) AS"
            + " value,ROW_NUMBER() OVER(ORDER BY COALESCE(m.like_count,0) DESC,n.id DESC) AS"
            + " position FROM t_note n LEFT JOIN t_note_metric m ON m.note_id=n.id WHERE "
            + (author == null ? "n.status='PUBLISHED'" : "n.author_id=? AND n.status<>'DELETED'");
    String sql = "SELECT * FROM (" + base + ") ranked WHERE position>? ORDER BY position LIMIT ?";
    return author == null ? db.query(sql, offset, size) : db.query(sql, author, offset, size);
  }

  public List<Map<String, Object>> notes(long after, int size) {
    return db.query("SELECT id FROM t_note WHERE id>? ORDER BY id LIMIT ?", after, size);
  }

  public void reconcileAll() {
    db.update(
        "INSERT INTO"
            + " t_note_metric(note_id,view_count,like_count,favorite_count,comment_count,calculated_at)"
            + " SELECT"
            + " n.id,COALESCE(v.views,0),COALESCE(l.likes,0),COALESCE(f.favorites,0),COALESCE(c.comments,0),UTC_TIMESTAMP(3)"
            + " FROM t_note n LEFT JOIN (SELECT subject_id,SUM(value) AS views FROM t_daily_metric"
            + " WHERE scope='NOTE' AND metric='VIEW' GROUP BY subject_id) v ON v.subject_id=n.id"
            + " LEFT JOIN (SELECT note_id,COUNT(*) AS likes FROM t_like GROUP BY note_id) l ON"
            + " l.note_id=n.id LEFT JOIN (SELECT note_id,COUNT(DISTINCT user_id) AS favorites FROM"
            + " t_favorite GROUP BY note_id) f ON f.note_id=n.id LEFT JOIN (SELECT note_id,COUNT(*)"
            + " AS comments FROM t_comment WHERE status='VISIBLE' GROUP BY note_id) c ON"
            + " c.note_id=n.id ON DUPLICATE KEY UPDATE"
            + " view_count=VALUES(view_count),like_count=VALUES(like_count),favorite_count=VALUES(favorite_count),comment_count=VALUES(comment_count),calculated_at=VALUES(calculated_at)");
  }
}
