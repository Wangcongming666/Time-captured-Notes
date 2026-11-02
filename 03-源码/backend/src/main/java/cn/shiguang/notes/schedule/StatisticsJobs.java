package cn.shiguang.notes.schedule;

import cn.shiguang.notes.service.StatisticsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Scheduled current-count reconciliation repairs missed derived-cache updates. */
@Component
public class StatisticsJobs {
  private final StatisticsService statistics;

  public StatisticsJobs(StatisticsService statistics) {
    this.statistics = statistics;
  }

  @Scheduled(fixedDelay = 30000, initialDelay = 20000)
  @Transactional
  public void reconcile() {
    statistics.reconcile();
  }
}
