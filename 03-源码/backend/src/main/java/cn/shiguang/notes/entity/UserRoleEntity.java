package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_user_role; never used as an HTTP response. */
@TableName("t_user_role")
public class UserRoleEntity extends BaseEntity {
  @TableField("user_id")
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long value) {
    userId = value;
  }

  @TableField("role_id")
  private Long roleId;

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long value) {
    roleId = value;
  }
}
