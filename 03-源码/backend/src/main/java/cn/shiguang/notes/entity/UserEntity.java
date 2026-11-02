package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_user; never used as an HTTP response. */
@TableName("t_user")
public class UserEntity extends BaseEntity {
  @TableField("username")
  private String username;

  public String getUsername() {
    return username;
  }

  public void setUsername(String value) {
    username = value;
  }

  @TableField("password_hash")
  private String passwordHash;

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String value) {
    passwordHash = value;
  }

  @TableField("nickname")
  private String nickname;

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String value) {
    nickname = value;
  }

  @TableField("bio")
  private String bio;

  public String getBio() {
    return bio;
  }

  public void setBio(String value) {
    bio = value;
  }

  @TableField("avatar_media_id")
  private Long avatarMediaId;

  public Long getAvatarMediaId() {
    return avatarMediaId;
  }

  public void setAvatarMediaId(Long value) {
    avatarMediaId = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @TableField("auth_version")
  private Integer authVersion;

  public Integer getAuthVersion() {
    return authVersion;
  }

  public void setAuthVersion(Integer value) {
    authVersion = value;
  }
}
