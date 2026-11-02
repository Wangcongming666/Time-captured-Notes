package cn.shiguang.notes.dto;

import org.slf4j.MDC;

/** Uniform response envelope with trace correlation. */
public record ApiResponse<T>(String code, String message, T data, String traceId) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>("OK", "成功", data, MDC.get("traceId"));
  }
}
