package cn.shiguang.notes.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record ReportVO(
    String id,
    String targetType,
    String targetId,
    String status,
    int version,
    String resultReason,
    @JsonInclude(JsonInclude.Include.NON_NULL) String reason) {}
