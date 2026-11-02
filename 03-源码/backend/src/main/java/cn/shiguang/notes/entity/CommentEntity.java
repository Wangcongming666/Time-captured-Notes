package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_comment; never used as an HTTP response. */
@TableName("t_comment")
public class CommentEntity extends BaseEntity {
  @TableField("author_id")
  private Long authorId;

  public Long getAuthorId() {
    return authorId;
  }

  public void setAuthorId(Long value) {
    authorId = value;
  }

  @TableField("note_id")
  private Long noteId;

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long value) {
    noteId = value;
  }

  @TableField("root_id")
  private Long rootId;

  public Long getRootId() {
    return rootId;
  }

  public void setRootId(Long value) {
    rootId = value;
  }

  @TableField("reply_to_id")
  private Long replyToId;

  public Long getReplyToId() {
    return replyToId;
  }

  public void setReplyToId(Long value) {
    replyToId = value;
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

  @TableField("version")
  private Integer version;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer value) {
    version = value;
  }
}
