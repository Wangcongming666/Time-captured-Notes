package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record ReviewDetailVO(ReviewVO review, OwnedNoteVO note, PracticeVO practice) {}
