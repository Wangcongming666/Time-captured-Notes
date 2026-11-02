package cn.shiguang.notes.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record RankVO(
    String subjectId,
    String label,
    String metric,
    long value,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long position,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long noteCount) {}
