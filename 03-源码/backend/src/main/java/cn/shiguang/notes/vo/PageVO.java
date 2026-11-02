package cn.shiguang.notes.vo;

import java.util.List;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record PageVO<T>(List<T> items, String nextCursor, boolean hasMore) {}
