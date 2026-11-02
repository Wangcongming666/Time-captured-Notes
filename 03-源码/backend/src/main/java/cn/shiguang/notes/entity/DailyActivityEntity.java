package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;

/** Persistent row of t_daily_activity; never used as an HTTP response. */
@TableName("t_daily_activity")
public class DailyActivityEntity extends BaseEntity {
  @TableField("day")
  private LocalDate day;

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate value) {
    day = value;
  }

  @TableField("user_id")
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long value) {
    userId = value;
  }
}
