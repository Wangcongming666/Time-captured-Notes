package cn.shiguang.notes.vo;

import java.util.List;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record DashboardVO(
    List<MetricVO> points, String calculatedAt, String timezone, List<RankVO> topicRanking) {}
