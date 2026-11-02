package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_topic; never used as an HTTP response. */
@TableName("t_topic")
public class TopicEntity extends BaseEntity {
  @TableField("name")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  @TableField("category")
  private String category;

  public String getCategory() {
    return category;
  }

  public void setCategory(String value) {
    category = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }
}
