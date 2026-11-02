package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_media; never used as an HTTP response. */
@TableName("t_media")
public class MediaEntity extends BaseEntity {
  @TableField("owner_id")
  private Long ownerId;

  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long value) {
    ownerId = value;
  }

  @TableField("storage_key")
  private String storageKey;

  public String getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(String value) {
    storageKey = value;
  }

  @TableField("purpose")
  private String purpose;

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String value) {
    purpose = value;
  }

  @TableField("mime")
  private String mime;

  public String getMime() {
    return mime;
  }

  public void setMime(String value) {
    mime = value;
  }

  @TableField("size_bytes")
  private Long sizeBytes;

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long value) {
    sizeBytes = value;
  }

  @TableField("width")
  private Integer width;

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer value) {
    width = value;
  }

  @TableField("height")
  private Integer height;

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer value) {
    height = value;
  }

  @TableField("sha256")
  private String sha256;

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String value) {
    sha256 = value;
  }

  @TableField("status")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @TableField("media_type")
  private String mediaType;

  public String getMediaType() {
    return mediaType;
  }

  public void setMediaType(String value) {
    mediaType = value;
  }

  @TableField("duration_ms")
  private Long durationMs;

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long value) {
    durationMs = value;
  }

  @TableField("poster_storage_key")
  private String posterStorageKey;

  public String getPosterStorageKey() {
    return posterStorageKey;
  }

  public void setPosterStorageKey(String value) {
    posterStorageKey = value;
  }

  @TableField("poster_size_bytes")
  private Long posterSizeBytes;

  public Long getPosterSizeBytes() {
    return posterSizeBytes;
  }

  public void setPosterSizeBytes(Long value) {
    posterSizeBytes = value;
  }
}
