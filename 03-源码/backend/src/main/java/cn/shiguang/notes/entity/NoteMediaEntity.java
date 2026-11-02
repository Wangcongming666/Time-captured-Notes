package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_note_media; never used as an HTTP response. */
@TableName("t_note_media")
public class NoteMediaEntity extends BaseEntity {
  @TableField("note_id")
  private Long noteId;

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long value) {
    noteId = value;
  }

  @TableField("media_id")
  private Long mediaId;

  public Long getMediaId() {
    return mediaId;
  }

  public void setMediaId(Long value) {
    mediaId = value;
  }

  @TableField("position")
  private Integer position;

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer value) {
    position = value;
  }
}
