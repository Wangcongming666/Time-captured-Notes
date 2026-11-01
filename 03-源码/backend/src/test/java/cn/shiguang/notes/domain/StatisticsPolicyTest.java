package cn.shiguang.notes.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.modules.statistics.application.StatisticsService;
import cn.shiguang.notes.modules.statistics.domain.StatisticsRepository;
import cn.shiguang.notes.shared.contract.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StatisticsPolicyTest {
  @Test
  void favoriteActionsContributeToAuthorAndPlatformTrends() {
    var repo = mock(StatisticsRepository.class);
    var service = new StatisticsService(repo, mock(EventPort.class));
    service.receive(
        Data.map(
            "eventType",
            "FavoriteChanged",
            "occurredAt",
            "2026-11-01T00:00:00Z",
            "payload",
            Data.map("noteId", "1", "authorId", "3", "desired", true)));
    verify(repo).metric("2026-11-01", "AUTHOR", 3, "FAVORITE_ADD", 1, true);
    verify(repo).metric("2026-11-01", "PLATFORM", 0, "FAVORITE_ADD", 1, true);
  }

  @Test
  void platformDashboardIncludesExplicitTopicRanking() {
    var service = new StatisticsService(mock(StatisticsRepository.class), mock(EventPort.class));
    assertTrue(
        service
            .dashboard(new Actor(1, Set.of("ADMIN")), "PLATFORM", 7)
            .containsKey("topicRanking"));
  }
}
