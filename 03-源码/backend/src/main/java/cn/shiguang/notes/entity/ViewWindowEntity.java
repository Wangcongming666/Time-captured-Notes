package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_view_window; never used as an HTTP response. */
@TableName("t_view_window")
public class ViewWindowEntity extends BaseEntity {
  @TableField("subject_hash")
  private String subjectHash;

  public String getSubjectHash() {
    return subjectHash;
  }

  public void setSubjectHash(String value) {
    subjectHash = value;
  }

  @TableField("note_id")
  private Long noteId;

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long value) {
    noteId = value;
  }

  @TableField("window_start")
  private LocalDateTime windowStart;

  public LocalDateTime getWindowStart() {
    return windowStart;
  }

  public void setWindowStart(LocalDateTime value) {
    windowStart = value;
  }
}
