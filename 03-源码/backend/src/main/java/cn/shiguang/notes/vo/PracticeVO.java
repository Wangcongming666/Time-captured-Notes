package cn.shiguang.notes.vo;

import java.util.List;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record PracticeVO(
    String id,
    String noteId,
    String status,
    int version,
    String publicStatus,
    String resultText,
    String helpfulness,
    boolean sourceAvailable,
    List<String> mediaIds) {}
