package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record ImageUploadVO(String id, String mime, long sizeBytes, int width, int height) {}
