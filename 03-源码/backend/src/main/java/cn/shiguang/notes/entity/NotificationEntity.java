package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_notification; never used as an HTTP response. */
@TableName("t_notification")
public class NotificationEntity extends BaseEntity {
  @TableField("recipient_id")
  private Long recipientId;

  public Long getRecipientId() {
    return recipientId;
  }

  public void setRecipientId(Long value) {
    recipientId = value;
  }

  @TableField("event_id")
  private String eventId;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String value) {
    eventId = value;
  }

  @TableField("type")
  private String type;

  public String getType() {
    return type;
  }

  public void setType(String value) {
    type = value;
  }

  @TableField("actor_id")
  private Long actorId;

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long value) {
    actorId = value;
  }

  @TableField("target_type")
  private String targetType;

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String value) {
    targetType = value;
  }

  @TableField("target_id")
  private Long targetId;

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long value) {
    targetId = value;
  }

  @TableField("read_at")
  private LocalDateTime readAt;

  public LocalDateTime getReadAt() {
    return readAt;
  }

  public void setReadAt(LocalDateTime value) {
    readAt = value;
  }
}
