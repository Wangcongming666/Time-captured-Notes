package cn.shiguang.notes.modules.content.domain;

import java.util.*;

/**
 * Repository methods describe content operations rather than expose SQL to application services.
 */
public interface ContentRepository {
  Map<String, Object> note(long id, boolean lock);

  List<Long> mediaIds(long id);

  List<Map<String, Object>> mediaEntries(long id);

  List<Long> topicIds(long id);

  List<Map<String, Object>> owned(long author, long before, int size);

  long create(long author, String title, String body);

  void edit(long id, String title, String body, String status, int version);

  void state(long id, String status, String reason, boolean newSubmission);

  void topics(long id, List<Long> topics);

  boolean activeTopics(List<Long> ids);

  Map<String, Object> media(long id, boolean lock);

  long addMedia(Map<String, Object> values);

  void bind(long owner, List<Long> ids, String purpose, String type, long target);

  void avatar(long owner, Long next, Long previous);
}
