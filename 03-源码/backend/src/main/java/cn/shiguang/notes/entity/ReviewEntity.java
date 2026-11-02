package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_review; never used as an HTTP response. */
@TableName("t_review")
public class ReviewEntity extends BaseEntity {
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

  @TableField("target_revision")
  private Integer targetRevision;

  public Integer getTargetRevision() {
    return targetRevision;
  }

  public void setTargetRevision(Integer value) {
    targetRevision = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @TableField("applicant_id")
  private Long applicantId;

  public Long getApplicantId() {
    return applicantId;
  }

  public void setApplicantId(Long value) {
    applicantId = value;
  }

  @TableField("reviewer_id")
  private Long reviewerId;

  public Long getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(Long value) {
    reviewerId = value;
  }

  @TableField("reason")
  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
  }

  @TableField("decided_at")
  private LocalDateTime decidedAt;

  public LocalDateTime getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(LocalDateTime value) {
    decidedAt = value;
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
