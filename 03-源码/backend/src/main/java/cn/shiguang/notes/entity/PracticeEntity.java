package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_practice; never used as an HTTP response. */
@TableName("t_practice")
public class PracticeEntity extends BaseEntity {
  @TableField("owner_id")
  private Long ownerId;

  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long value) {
    ownerId = value;
  }

  @TableField("note_id")
  private Long noteId;

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long value) {
    noteId = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @TableField("version")
  private Integer version;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer value) {
    version = value;
  }

  @TableField("result_text")
  private String resultText;

  public String getResultText() {
    return resultText;
  }

  public void setResultText(String value) {
    resultText = value;
  }

  @TableField("helpfulness")
  private String helpfulness;

  public String getHelpfulness() {
    return helpfulness;
  }

  public void setHelpfulness(String value) {
    helpfulness = value;
  }

  @TableField("public_status")
  private String publicStatus;

  public String getPublicStatus() {
    return publicStatus;
  }

  public void setPublicStatus(String value) {
    publicStatus = value;
  }

  @TableField("feedback_revision")
  private Integer feedbackRevision;

  public Integer getFeedbackRevision() {
    return feedbackRevision;
  }

  public void setFeedbackRevision(Integer value) {
    feedbackRevision = value;
  }

  @TableField("source_revision")
  private Integer sourceRevision;

  public Integer getSourceRevision() {
    return sourceRevision;
  }

  public void setSourceRevision(Integer value) {
    sourceRevision = value;
  }

  @TableField("started_at")
  private LocalDateTime startedAt;

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime value) {
    startedAt = value;
  }

  @TableField("finished_at")
  private LocalDateTime finishedAt;

  public LocalDateTime getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(LocalDateTime value) {
    finishedAt = value;
  }
}
