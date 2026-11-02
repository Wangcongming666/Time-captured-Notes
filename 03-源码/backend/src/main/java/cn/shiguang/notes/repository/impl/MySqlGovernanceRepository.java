package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.GovernanceRepository;
import cn.shiguang.notes.repository.support.EntityWrites;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlGovernanceRepository implements GovernanceRepository {
  private final AuditMapper auditMapper;
  private final ReportMapper reportMapper;
  private final ReviewMapper reviewMapper;
  private final TopicMapper topicMapper;

  public MySqlGovernanceRepository(
      AuditMapper auditMapper,
      ReportMapper reportMapper,
      ReviewMapper reviewMapper,
      TopicMapper topicMapper) {
    this.auditMapper = auditMapper;
    this.reportMapper = reportMapper;
    this.reviewMapper = reviewMapper;
    this.topicMapper = topicMapper;
  }

  public long openReview(String type, long id, int revision, long applicant) {
    return EntityWrites.insert(
        reviewMapper,
        ReviewEntity.class,
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
    return MapperRows.requiredEntity(reviewMapper.governanceReview(id, lock));
  }

  public void cancelReviews(String type, long id) {
    reviewMapper.governanceCancelReviews(type, id);
  }

  public void decide(long id, String status, long actor, String reason, int version) {
    if (reviewMapper.governanceDecide(status, actor, reason, id, version) != 1)
      throw BizException.conflict("审核任务已处理");
  }

  public List<Map<String, Object>> reviews(String status, long before, int size) {
    return MapperRows.list(reviewMapper.governanceReviews(status, before, size));
  }

  public long report(long actor, String type, long target, String reason, String key) {
    try {
      return EntityWrites.insert(
          reportMapper,
          ReportEntity.class,
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
      return Data.id(MapperRows.one(reportMapper.governanceReport(key)), "id");
    }
  }

  public Map<String, Object> report(long id, boolean lock) {
    return MapperRows.requiredEntity(reportMapper.governanceReport2(id, lock));
  }

  public void decideReport(long id, long actor, int version, String status, String reason) {
    boolean closed = Set.of("UPHELD", "REJECTED").contains(status);
    if (reportMapper.governanceDecideReport(status, actor, reason, closed, closed, id, version)
        != 1) throw BizException.conflict("工单已变化");
  }

  public List<Map<String, Object>> reports(Long user, long before, int size) {
    return user == null
        ? MapperRows.list(reportMapper.governanceReports(before, size))
        : MapperRows.list(reportMapper.governanceReports2(user, before, size));
  }

  public List<Map<String, Object>> topics(long before, int size) {
    return MapperRows.list(topicMapper.governanceTopics(before, size));
  }

  public List<Map<String, Object>> allTopics(long before, int size) {
    return MapperRows.list(topicMapper.governanceAllTopics(before, size));
  }

  public long createTopic(String name, String category, String status) {
    return EntityWrites.insert(
        topicMapper,
        TopicEntity.class,
        Data.map("name", name, "category", category, "status", status));
  }

  public Map<String, Object> updateTopic(long id, String name, String category, String status) {
    if (topicMapper.governanceUpdateTopic(name, category, status, id) != 1)
      throw BizException.missing();
    return MapperRows.one(topicMapper.governanceUpdateTopic2(id));
  }

  public void audit(Map<String, Object> fields) {
    EntityWrites.insert(auditMapper, AuditEntity.class, fields);
  }

  public List<Map<String, Object>> audits(long before, int size) {
    return MapperRows.list(auditMapper.governanceAudits(before, size));
  }
}
