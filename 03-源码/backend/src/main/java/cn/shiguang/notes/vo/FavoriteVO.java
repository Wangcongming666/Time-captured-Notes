package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record FavoriteVO(String id, String noteId, boolean available, String title) {}
