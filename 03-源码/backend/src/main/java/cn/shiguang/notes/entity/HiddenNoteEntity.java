package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_hidden_note; never used as an HTTP response. */
@TableName("t_hidden_note")
public class HiddenNoteEntity extends BaseEntity {
  @TableField("user_id")
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long value) {
    userId = value;
  }

  @TableField("note_id")
  private Long noteId;

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long value) {
    noteId = value;
  }
}
