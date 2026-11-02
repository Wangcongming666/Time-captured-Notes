package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_command; never used as an HTTP response. */
@TableName("t_command")
public class CommandEntity extends BaseEntity {
  @TableField("actor_id")
  private Long actorId;

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long value) {
    actorId = value;
  }

  @TableField("scope")
  private String scope;

  public String getScope() {
    return scope;
  }

  public void setScope(String value) {
    scope = value;
  }

  @TableField("request_key")
  private String requestKey;

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String value) {
    requestKey = value;
  }

  @TableField("request_hash")
  private String requestHash;

  public String getRequestHash() {
    return requestHash;
  }

  public void setRequestHash(String value) {
    requestHash = value;
  }

  @TableField("response_json")
  private String responseJson;

  public String getResponseJson() {
    return responseJson;
  }

  public void setResponseJson(String value) {
    responseJson = value;
  }

  @TableField("expires_at")
  private LocalDateTime expiresAt;

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime value) {
    expiresAt = value;
  }
}
