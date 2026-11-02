package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record CardVO(
    String id,
    String authorId,
    String authorName,
    String title,
    String coverMediaId,
    String coverMediaType,
    Long durationMs,
    String authorAvatarMediaId,
    String reason,
    double score,
    long likeCount,
    long favoriteCount) {}
