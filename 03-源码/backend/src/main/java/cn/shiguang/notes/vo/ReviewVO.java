package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record ReviewVO(
    String id,
    String targetType,
    String targetId,
    int targetRevision,
    int version,
    String status,
    String applicantId) {}
