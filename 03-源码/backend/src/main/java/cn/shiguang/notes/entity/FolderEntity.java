package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_folder; never used as an HTTP response. */
@TableName("t_folder")
public class FolderEntity extends BaseEntity {
  @TableField("owner_id")
  private Long ownerId;

  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long value) {
    ownerId = value;
  }

  @TableField("name")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  @TableField("version")
  private Integer version;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer value) {
    version = value;
  }
}
