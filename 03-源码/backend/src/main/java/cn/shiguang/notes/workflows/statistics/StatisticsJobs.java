package cn.shiguang.notes.workflows.statistics;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import cn.shiguang.notes.modules.statistics.api.StatisticsApi;
/** Scheduled current-count reconciliation repairs missed derived-cache updates. */
@Component
public class StatisticsJobs {
    private final StatisticsApi statistics;
    public StatisticsJobs(StatisticsApi statistics){this.statistics=statistics;}
    @Scheduled(fixedDelay=30000,initialDelay=20000) @Transactional public void reconcile(){statistics.reconcile();}
}
