package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record FollowVO(
    String relationId, String id, String nickname, String bio, String avatarMediaId) {}
