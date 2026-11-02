package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Persistent row of t_daily_metric; never used as an HTTP response. */
@TableName("t_daily_metric")
public class DailyMetricEntity extends BaseEntity {
  @TableField("day")
  private LocalDate day;

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate value) {
    day = value;
  }

  @TableField("scope")
  private String scope;

  public String getScope() {
    return scope;
  }

  public void setScope(String value) {
    scope = value;
  }

  @TableField("subject_id")
  private Long subjectId;

  public Long getSubjectId() {
    return subjectId;
  }

  public void setSubjectId(Long value) {
    subjectId = value;
  }

  @TableField("metric")
  private String metric;

  public String getMetric() {
    return metric;
  }

  public void setMetric(String value) {
    metric = value;
  }

  @TableField("value")
  private BigDecimal value;

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  @TableField("calculated_at")
  private LocalDateTime calculatedAt;

  public LocalDateTime getCalculatedAt() {
    return calculatedAt;
  }

  public void setCalculatedAt(LocalDateTime value) {
    calculatedAt = value;
  }
}
