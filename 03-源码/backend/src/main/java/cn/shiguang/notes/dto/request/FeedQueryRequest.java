package cn.shiguang.notes.dto.request;

/** Supported discovery query fields. Null means the client omitted the parameter. */
public record FeedQueryRequest(
    String mode,
    String cursor,
    String size,
    String category,
    String q,
    String topicId,
    String authorId,
    String sort) {}
