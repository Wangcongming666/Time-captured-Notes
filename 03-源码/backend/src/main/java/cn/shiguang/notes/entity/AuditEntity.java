package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_audit; never used as an HTTP response. */
@TableName("t_audit")
public class AuditEntity extends BaseEntity {
  @TableField("actor_id")
  private Long actorId;

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long value) {
    actorId = value;
  }

  @TableField("action")
  private String action;

  public String getAction() {
    return action;
  }

  public void setAction(String value) {
    action = value;
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

  @TableField("before_json")
  private String beforeJson;

  public String getBeforeJson() {
    return beforeJson;
  }

  public void setBeforeJson(String value) {
    beforeJson = value;
  }

  @TableField("after_json")
  private String afterJson;

  public String getAfterJson() {
    return afterJson;
  }

  public void setAfterJson(String value) {
    afterJson = value;
  }

  @TableField("reason")
  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
  }

  @TableField("trace_id")
  private String traceId;

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String value) {
    traceId = value;
  }
}
