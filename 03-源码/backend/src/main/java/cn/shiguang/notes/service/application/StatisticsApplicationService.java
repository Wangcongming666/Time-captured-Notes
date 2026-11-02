package cn.shiguang.notes.service.application;

import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.service.support.*;
import cn.shiguang.notes.vo.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Statistics use cases: authorization, transaction orchestration, and typed public projections. */
@Service
public class StatisticsApplicationService {
  private final StatisticsService statistics;
  private final Pages pages;
  private final HttpBoundaryConverter views;

  public StatisticsApplicationService(
      StatisticsService statistics, Pages pages, HttpBoundaryConverter views) {
    this.statistics = statistics;
    this.pages = pages;
    this.views = views;
  }

  public DashboardVO dashboard(Actor a, String scope, int days) {
    return views.view(statistics.dashboard(a, scope, days), DashboardVO.class);
  }

  public PageVO<RankVO> ranking(Actor a, boolean platform, String cursor, int size) {
    return views.page(
        pages.fetch(
            "ranking:" + platform,
            a.id(),
            cursor,
            size,
            "position",
            (b, s) -> statistics.ranking(a, platform, b, s)),
        RankVO.class);
  }
}
