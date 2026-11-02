package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record MetricVO(String day, String metric, java.math.BigDecimal value) {}
