package cn.shiguang.notes.vo;

import java.util.List;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record FeedbackVO(
    String id,
    String noteId,
    String authorId,
    String resultText,
    String helpfulness,
    List<String> mediaIds) {}
