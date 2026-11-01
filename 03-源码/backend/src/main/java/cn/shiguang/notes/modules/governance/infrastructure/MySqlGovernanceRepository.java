package cn.shiguang.notes.modules.governance.infrastructure;

import cn.shiguang.notes.modules.governance.domain.GovernanceRepository;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.Data;
import cn.shiguang.notes.shared.error.BizException;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlGovernanceRepository implements GovernanceRepository {
  private final Database db;

  public MySqlGovernanceRepository(Database db) {
    this.db = db;
  }

  public long openReview(String type, long id, int revision, long applicant) {
    return db.insert(
        "t_review",
        Data.map(
            "target_type",
            type,
            "target_id",
            id,
            "target_revision",
            revision,
            "applicant_id",
            applicant));
  }

  public Map<String, Object> review(long id, boolean lock) {
    return db.one("SELECT * FROM t_review WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public void cancelReviews(String type, long id) {
    db.update(
        "UPDATE t_review SET status='CANCELLED',version=version+1 WHERE target_type=? AND"
            + " target_id=? AND status='PENDING'",
        type,
        id);
  }

  public void decide(long id, String status, long actor, String reason, int version) {
    if (db.update(
            "UPDATE t_review SET"
                + " status=?,reviewer_id=?,reason=?,decided_at=UTC_TIMESTAMP(3),version=version+1"
                + " WHERE id=? AND version=? AND status='PENDING'",
            status,
            actor,
            reason,
            id,
            version)
        != 1) throw BizException.conflict("审核任务已处理");
  }

  public List<Map<String, Object>> reviews(String status, long before, int size) {
    return db.query(
        "SELECT id,target_type,target_id,target_revision,version,status,applicant_id FROM t_review"
            + " WHERE status=? AND id<? ORDER BY id DESC LIMIT ?",
        status,
        before,
        size);
  }

  public long report(long actor, String type, long target, String reason, String key) {
    try {
      return db.insert(
          "t_report",
          Data.map(
              "reporter_id",
              actor,
              "target_type",
              type,
              "target_id",
              target,
              "reason",
              reason,
              "open_key",
              key));
    } catch (DuplicateKeyException e) {
      return Data.id(db.one("SELECT id FROM t_report WHERE open_key=?", key), "id");
    }
  }

  public Map<String, Object> report(long id, boolean lock) {
    return db.one("SELECT * FROM t_report WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public void decideReport(long id, long actor, int version, String status, String reason) {
    boolean closed = Set.of("UPHELD", "REJECTED").contains(status);
    if (db.update(
            "UPDATE t_report SET"
                + " status=?,handler_id=?,result_reason=?,version=version+1,open_key=IF(?,NULL,open_key),closed_at=IF(?,UTC_TIMESTAMP(3),NULL)"
                + " WHERE id=? AND version=?",
            status,
            actor,
            reason,
            closed,
            closed,
            id,
            version)
        != 1) throw BizException.conflict("工单已变化");
  }

  public List<Map<String, Object>> reports(Long user, long before, int size) {
    return user == null
        ? db.query(
            "SELECT id,target_type,target_id,reason,status,version,result_reason FROM t_report"
                + " WHERE id<? ORDER BY id DESC LIMIT ?",
            before,
            size)
        : db.query(
            "SELECT id,target_type,target_id,status,version,result_reason FROM t_report WHERE"
                + " reporter_id=? AND id<? ORDER BY id DESC LIMIT ?",
            user,
            before,
            size);
  }

  public List<Map<String, Object>> topics(long before, int size) {
    return db.query(
        "SELECT id,name,category,status FROM t_topic WHERE status='ACTIVE' AND id<? ORDER BY id"
            + " DESC LIMIT ?",
        before,
        size);
  }

  public List<Map<String, Object>> allTopics(long before, int size) {
    return db.query(
        "SELECT id,name,category,status FROM t_topic WHERE id<? ORDER BY id DESC LIMIT ?",
        before,
        size);
  }

  public long createTopic(String name, String category, String status) {
    return db.insert("t_topic", Data.map("name", name, "category", category, "status", status));
  }

  public Map<String, Object> updateTopic(long id, String name, String category, String status) {
    if (db.update(
            "UPDATE t_topic SET name=?,category=?,status=? WHERE id=?", name, category, status, id)
        != 1) throw BizException.missing();
    return db.one("SELECT id,name,category,status FROM t_topic WHERE id=?", id);
  }

  public void audit(Map<String, Object> fields) {
    db.insert("t_audit", fields);
  }

  public List<Map<String, Object>> audits(long before, int size) {
    return db.query(
        "SELECT id,actor_id,action,target_type,target_id,reason,trace_id,created_at FROM t_audit"
            + " WHERE id<? ORDER BY id DESC LIMIT ?",
        before,
        size);
  }
}
