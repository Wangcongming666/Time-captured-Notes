package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_follow; never used as an HTTP response. */
@TableName("t_follow")
public class FollowEntity extends BaseEntity {
  @TableField("follower_id")
  private Long followerId;

  public Long getFollowerId() {
    return followerId;
  }

  public void setFollowerId(Long value) {
    followerId = value;
  }

  @TableField("followee_id")
  private Long followeeId;

  public Long getFolloweeId() {
    return followeeId;
  }

  public void setFolloweeId(Long value) {
    followeeId = value;
  }
}
