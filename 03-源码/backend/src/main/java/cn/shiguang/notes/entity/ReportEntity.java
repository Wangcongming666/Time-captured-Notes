package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_report; never used as an HTTP response. */
@TableName("t_report")
public class ReportEntity extends BaseEntity {
  @TableField("reporter_id")
  private Long reporterId;

  public Long getReporterId() {
    return reporterId;
  }

  public void setReporterId(Long value) {
    reporterId = value;
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

  @TableField("reason")
  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @TableField("open_key")
  private String openKey;

  public String getOpenKey() {
    return openKey;
  }

  public void setOpenKey(String value) {
    openKey = value;
  }

  @TableField("handler_id")
  private Long handlerId;

  public Long getHandlerId() {
    return handlerId;
  }

  public void setHandlerId(Long value) {
    handlerId = value;
  }

  @TableField("result_reason")
  private String resultReason;

  public String getResultReason() {
    return resultReason;
  }

  public void setResultReason(String value) {
    resultReason = value;
  }

  @TableField("closed_at")
  private LocalDateTime closedAt;

  public LocalDateTime getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(LocalDateTime value) {
    closedAt = value;
  }

  @TableField("version")
  private Integer version;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer value) {
    version = value;
  }
}
