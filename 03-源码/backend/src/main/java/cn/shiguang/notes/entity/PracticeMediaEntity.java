package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_practice_media; never used as an HTTP response. */
@TableName("t_practice_media")
public class PracticeMediaEntity extends BaseEntity {
  @TableField("practice_id")
  private Long practiceId;

  public Long getPracticeId() {
    return practiceId;
  }

  public void setPracticeId(Long value) {
    practiceId = value;
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
