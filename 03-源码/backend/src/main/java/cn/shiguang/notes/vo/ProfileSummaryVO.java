package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record ProfileSummaryVO(
    long followingCount,
    long followerCount,
    long noteCount,
    long appreciationCount,
    boolean following) {}
