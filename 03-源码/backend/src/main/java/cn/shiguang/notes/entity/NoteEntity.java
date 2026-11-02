package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_note; never used as an HTTP response. */
@TableName("t_note")
public class NoteEntity extends BaseEntity {
  @TableField("author_id")
  private Long authorId;

  public Long getAuthorId() {
    return authorId;
  }

  public void setAuthorId(Long value) {
    authorId = value;
  }

  @TableField("title")
  private String title;

  public String getTitle() {
    return title;
  }

  public void setTitle(String value) {
    title = value;
  }

  @TableField("body")
  private String body;

  public String getBody() {
    return body;
  }

  public void setBody(String value) {
    body = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @TableField("revision")
  private Integer revision;

  public Integer getRevision() {
    return revision;
  }

  public void setRevision(Integer value) {
    revision = value;
  }

  @TableField("version")
  private Integer version;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer value) {
    version = value;
  }

  @TableField("published_at")
  private LocalDateTime publishedAt;

  public LocalDateTime getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(LocalDateTime value) {
    publishedAt = value;
  }

  @TableField("reject_reason")
  private String rejectReason;

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String value) {
    rejectReason = value;
  }
}
