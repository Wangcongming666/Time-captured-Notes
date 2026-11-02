package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_note_metric; never used as an HTTP response. */
@TableName("t_note_metric")
public class NoteMetricEntity extends BaseEntity {
  @TableField("note_id")
  private Long noteId;

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long value) {
    noteId = value;
  }

  @TableField("view_count")
  private Long viewCount;

  public Long getViewCount() {
    return viewCount;
  }

  public void setViewCount(Long value) {
    viewCount = value;
  }

  @TableField("like_count")
  private Long likeCount;

  public Long getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(Long value) {
    likeCount = value;
  }

  @TableField("favorite_count")
  private Long favoriteCount;

  public Long getFavoriteCount() {
    return favoriteCount;
  }

  public void setFavoriteCount(Long value) {
    favoriteCount = value;
  }

  @TableField("comment_count")
  private Long commentCount;

  public Long getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(Long value) {
    commentCount = value;
  }

  @TableField("calculated_at")
  private LocalDateTime calculatedAt;

  public LocalDateTime getCalculatedAt() {
    return calculatedAt;
  }

  public void setCalculatedAt(LocalDateTime value) {
    calculatedAt = value;
  }
}
