package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record NotificationVO(
    String id,
    String type,
    String actorId,
    String targetType,
    String targetId,
    boolean targetAvailable,
    String readAt,
    String createdAt) {}
