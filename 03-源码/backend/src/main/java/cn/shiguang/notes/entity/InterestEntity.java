package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_interest; never used as an HTTP response. */
@TableName("t_interest")
public class InterestEntity extends BaseEntity {
  @TableField("user_id")
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long value) {
    userId = value;
  }

  @TableField("topic_id")
  private Long topicId;

  public Long getTopicId() {
    return topicId;
  }

  public void setTopicId(Long value) {
    topicId = value;
  }

  @TableField("mode")
  private String mode;

  public String getMode() {
    return mode;
  }

  public void setMode(String value) {
    mode = value;
  }
}
