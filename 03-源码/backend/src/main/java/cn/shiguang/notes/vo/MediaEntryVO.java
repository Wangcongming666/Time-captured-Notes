package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record MediaEntryVO(
    String id, String mediaType, String mime, int width, int height, Long durationMs) {}
