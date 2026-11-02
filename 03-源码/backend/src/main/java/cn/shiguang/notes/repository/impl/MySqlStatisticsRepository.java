package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.StatisticsRepository;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlStatisticsRepository implements StatisticsRepository {
  public List<Map<String, Object>> hotTopics(int days) {
    return MapperRows.list(topicMapper.statisticsHotTopics(days));
  }

  private final DailyActivityMapper dailyActivityMapper;
  private final DailyMetricMapper dailyMetricMapper;
  private final NoteMapper noteMapper;
  private final NoteMetricMapper noteMetricMapper;
  private final ReviewMapper reviewMapper;
  private final TopicMapper topicMapper;
  private final ViewWindowMapper viewWindowMapper;

  public MySqlStatisticsRepository(
      DailyActivityMapper dailyActivityMapper,
      DailyMetricMapper dailyMetricMapper,
      NoteMapper noteMapper,
      NoteMetricMapper noteMetricMapper,
      ReviewMapper reviewMapper,
      TopicMapper topicMapper,
      ViewWindowMapper viewWindowMapper) {
    this.dailyActivityMapper = dailyActivityMapper;
    this.dailyMetricMapper = dailyMetricMapper;
    this.noteMapper = noteMapper;
    this.noteMetricMapper = noteMetricMapper;
    this.reviewMapper = reviewMapper;
    this.topicMapper = topicMapper;
    this.viewWindowMapper = viewWindowMapper;
  }

  public boolean view(String subject, long note) {
    return viewWindowMapper.statisticsView(subject, note) > 0;
  }

  public void active(long user) {
    dailyActivityMapper.statisticsActive(user);
  }

  public void incrementView(long note, long author, String day) {
    metric(day, "NOTE", note, "VIEW", 1, true);
    metric(day, "AUTHOR", author, "VIEW", 1, true);
    metric(day, "PLATFORM", 0, "VIEW", 1, true);
  }

  public void metric(
      String day, String scope, long subject, String metric, double value, boolean increment) {
    dailyMetricMapper.statisticsMetric(day, scope, subject, metric, value, increment);
  }

  public void recalculate(long note) {
    noteMetricMapper.statisticsRecalculate(note, note, note, note, note);
  }

  public List<Map<String, Object>> points(String scope, long subject, int days) {
    var result = MapperRows.list(dailyMetricMapper.statisticsPoints(scope, subject, days - 1));
    if (scope.equals("PLATFORM"))
      result.addAll(MapperRows.list(dailyActivityMapper.statisticsPoints(days - 1)));
    return result;
  }

  public List<Map<String, Object>> governance(int days) {
    return MapperRows.list(reviewMapper.statisticsGovernance(days, days, days, days, days, days));
  }

  public List<Map<String, Object>> ranking(Long author, long before, int size) {
    return MapperRows.list(
        noteMapper.statisticsRanking(author, before == Long.MAX_VALUE ? 0 : before, size));
  }

  public List<Map<String, Object>> notes(long after, int size) {
    return MapperRows.list(noteMapper.statisticsNotes(after, size));
  }

  public void reconcileAll() {
    noteMetricMapper.statisticsReconcileAll();
  }
}
