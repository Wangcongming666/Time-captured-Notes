package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_favorite; never used as an HTTP response. */
@TableName("t_favorite")
public class FavoriteEntity extends BaseEntity {
  @TableField("folder_id")
  private Long folderId;

  public Long getFolderId() {
    return folderId;
  }

  public void setFolderId(Long value) {
    folderId = value;
  }

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
