package cn.shiguang.notes.repository;

import java.util.*;

public interface GovernanceRepository {
  long openReview(String type, long target, int revision, long applicant);

  Map<String, Object> review(long id, boolean lock);

  void cancelReviews(String type, long target);

  void decide(long id, String status, long actor, String reason, int version);

  List<Map<String, Object>> reviews(String status, long before, int size);

  long report(long actor, String type, long target, String reason, String openKey);

  Map<String, Object> report(long id, boolean lock);

  void decideReport(long id, long actor, int version, String status, String reason);

  List<Map<String, Object>> reports(Long user, long before, int size);

  List<Map<String, Object>> topics(long before, int size);

  List<Map<String, Object>> allTopics(long before, int size);

  long createTopic(String name, String category, String status);

  Map<String, Object> updateTopic(long id, String name, String category, String status);

  void audit(Map<String, Object> fields);

  List<Map<String, Object>> audits(long before, int size);
}
