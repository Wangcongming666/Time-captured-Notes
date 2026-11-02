package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_inbox; never used as an HTTP response. */
@TableName("t_inbox")
public class InboxEntity extends BaseEntity {
  @TableField("consumer")
  private String consumer;

  public String getConsumer() {
    return consumer;
  }

  public void setConsumer(String value) {
    consumer = value;
  }

  @TableField("event_id")
  private String eventId;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String value) {
    eventId = value;
  }

  @TableField("processed_at")
  private LocalDateTime processedAt;

  public LocalDateTime getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(LocalDateTime value) {
    processedAt = value;
  }
}
