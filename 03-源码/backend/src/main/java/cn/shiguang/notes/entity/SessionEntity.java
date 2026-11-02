package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/** Persistent row of t_session; never used as an HTTP response. */
@TableName("t_session")
public class SessionEntity extends BaseEntity {
  @TableField("token_hash")
  private String tokenHash;

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String value) {
    tokenHash = value;
  }

  @TableField("user_id")
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long value) {
    userId = value;
  }

  @TableField("auth_version")
  private Integer authVersion;

  public Integer getAuthVersion() {
    return authVersion;
  }

  public void setAuthVersion(Integer value) {
    authVersion = value;
  }

  @TableField("csrf_hash")
  private String csrfHash;

  public String getCsrfHash() {
    return csrfHash;
  }

  public void setCsrfHash(String value) {
    csrfHash = value;
  }

  @TableField("expires_at")
  private LocalDateTime expiresAt;

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime value) {
    expiresAt = value;
  }

  @TableField("revoked_at")
  private LocalDateTime revokedAt;

  public LocalDateTime getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(LocalDateTime value) {
    revokedAt = value;
  }
}
