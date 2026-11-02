package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_role; never used as an HTTP response. */
@TableName("t_role")
public class RoleEntity extends BaseEntity {
  @TableField("code")
  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    code = value;
  }

  @TableField("name")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }
}
