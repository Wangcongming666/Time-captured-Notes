package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record HistoryVO(String id, String fromStatus, String toStatus, String createdAt) {}
