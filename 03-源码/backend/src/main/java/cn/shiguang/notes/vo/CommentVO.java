package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record CommentVO(
    String id,
    String authorId,
    String noteId,
    String rootId,
    String replyToId,
    String body,
    String status,
    String createdAt) {}
