package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.DiscoveryRepository;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlDiscoveryRepository implements DiscoveryRepository {
  private final HiddenNoteMapper hiddenNoteMapper;
  private final InterestMapper interestMapper;
  private final NoteMapper noteMapper;
  private final TopicMapper topicMapper;

  public MySqlDiscoveryRepository(
      HiddenNoteMapper hiddenNoteMapper,
      InterestMapper interestMapper,
      NoteMapper noteMapper,
      TopicMapper topicMapper) {
    this.hiddenNoteMapper = hiddenNoteMapper;
    this.interestMapper = interestMapper;
    this.noteMapper = noteMapper;
    this.topicMapper = topicMapper;
  }

  public List<Map<String, Object>> candidates(
      long user, String mode, String category, String keyword, Long topic, Long author) {
    if (keyword != null && keyword.length() > 100) throw BizException.invalid("关键词过长");
    String pattern =
        keyword == null || keyword.isBlank()
            ? null
            : "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    return MapperRows.list(
        noteMapper.discoveryCandidates(
            user,
            mode,
            category == null || category.isBlank() ? null : category,
            pattern,
            topic,
            author));
  }

  public Set<String> visible(long user, List<String> ids) {
    if (ids.isEmpty()) return Set.of();
    return new HashSet<>(
        MapperRows.list(noteMapper.discoveryVisible(user, ids)).stream()
            .map(row -> Data.text(row, "id"))
            .toList());
  }

  public Map<String, Object> preferences(long user) {
    var rows = MapperRows.list(interestMapper.discoveryPreferences(user));
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
    interestMapper.discoveryPreferences2(user);
    for (long t : likes) interest(user, t, "LIKE");
    for (long t : reduced) interest(user, t, "REDUCE");
  }

  private void interest(long user, long topic, String mode) {
    if (topicMapper.discoveryInterest(topic) != 1) throw BizException.invalid("无效话题");
    interestMapper.discoveryInterest(user, topic, mode);
  }

  public void hide(long user, long note, boolean hidden) {
    if (hidden) hiddenNoteMapper.discoveryHide(user, note);
    else hiddenNoteMapper.discoveryHide2(user, note);
  }

  public void reset(long user) {
    interestMapper.discoveryReset(user);
    hiddenNoteMapper.discoveryReset(user);
  }
}
