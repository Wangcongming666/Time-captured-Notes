package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record AuditVO(
    String id,
    String actorId,
    String action,
    String targetType,
    String targetId,
    String reason,
    String traceId,
    String createdAt) {}
