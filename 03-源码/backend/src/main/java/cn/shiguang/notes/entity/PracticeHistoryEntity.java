package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_practice_history; never used as an HTTP response. */
@TableName("t_practice_history")
public class PracticeHistoryEntity extends BaseEntity {
  @TableField("practice_id")
  private Long practiceId;

  public Long getPracticeId() {
    return practiceId;
  }

  public void setPracticeId(Long value) {
    practiceId = value;
  }

  @TableField("from_status")
  private String fromStatus;

  public String getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(String value) {
    fromStatus = value;
  }

  @TableField("to_status")
  private String toStatus;

  public String getToStatus() {
    return toStatus;
  }

  public void setToStatus(String value) {
    toStatus = value;
  }

  @TableField("actor_id")
  private Long actorId;

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long value) {
    actorId = value;
  }
}
