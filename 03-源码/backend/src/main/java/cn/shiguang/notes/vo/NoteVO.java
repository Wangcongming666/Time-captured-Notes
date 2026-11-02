package cn.shiguang.notes.vo;

import java.util.List;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record NoteVO(
    String id,
    String authorId,
    String title,
    String body,
    List<String> mediaIds,
    List<MediaEntryVO> mediaEntries,
    List<String> topicIds,
    String publishedAt,
    boolean liked,
    boolean following,
    boolean favorite,
    long likeCount,
    long favoriteCount,
    long commentCount) {}
