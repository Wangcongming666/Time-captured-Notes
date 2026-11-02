package cn.shiguang.notes.vo;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record MediaCapabilitiesVO(
    long videoMaxBytes,
    int videoMaxSeconds,
    long imageMaxBytes,
    int imageMaxCount,
    String videoFormat) {}
