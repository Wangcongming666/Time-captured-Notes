package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_outbox; never used as an HTTP response. */
@TableName("t_outbox")
public class OutboxEntity extends BaseEntity {
  @TableField("event_id")
  private String eventId;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String value) {
    eventId = value;
  }

  @TableField("consumer")
  private String consumer;

  public String getConsumer() {
    return consumer;
  }

  public void setConsumer(String value) {
    consumer = value;
  }

  @TableField("routing_key")
  private String routingKey;

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String value) {
    routingKey = value;
  }

  @TableField("payload")
  private String payload;

  public String getPayload() {
    return payload;
  }

  public void setPayload(String value) {
    payload = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @TableField("attempts")
  private Integer attempts;

  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer value) {
    attempts = value;
  }

  @TableField("publish_attempts")
  private Integer publishAttempts;

  public Integer getPublishAttempts() {
    return publishAttempts;
  }

  public void setPublishAttempts(Integer value) {
    publishAttempts = value;
  }

  @TableField("available_at")
  private LocalDateTime availableAt;

  public LocalDateTime getAvailableAt() {
    return availableAt;
  }

  public void setAvailableAt(LocalDateTime value) {
    availableAt = value;
  }

  @TableField("lease_until")
  private LocalDateTime leaseUntil;

  public LocalDateTime getLeaseUntil() {
    return leaseUntil;
  }

  public void setLeaseUntil(LocalDateTime value) {
    leaseUntil = value;
  }

  @TableField("lease_token")
  private String leaseToken;

  public String getLeaseToken() {
    return leaseToken;
  }

  public void setLeaseToken(String value) {
    leaseToken = value;
  }

  @TableField("last_error")
  private String lastError;

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String value) {
    lastError = value;
  }
}
