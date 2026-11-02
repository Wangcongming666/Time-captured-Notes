package cn.shiguang.notes.controller.advice;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.dto.ApiResponse;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Safe HTTP errors; internal database and stack details are never returned. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BizException.class)
  public ResponseEntity<ApiResponse<Void>> business(BizException e) {
    return response(e.status(), e.code(), e.getMessage());
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class,
    org.springframework.web.bind.MethodArgumentNotValidException.class,
    org.springframework.validation.BindException.class,
    jakarta.validation.ConstraintViolationException.class,
    org.springframework.web.method.annotation.HandlerMethodValidationException.class
  })
  public ResponseEntity<ApiResponse<Void>> invalid(Exception e) {
    return response(400, "INVALID_INPUT", "请求格式或参数无效");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> upload() {
    return response(413, "FILE_TOO_LARGE", "单张图片不能超过10MB");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> method() {
    return response(405, "METHOD_NOT_ALLOWED", "该接口不支持此请求方法");
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> missing() {
    return response(404, "NOT_FOUND", "接口不存在");
  }

  @ExceptionHandler(org.springframework.web.bind.ServletRequestBindingException.class)
  public ResponseEntity<ApiResponse<Void>> binding() {
    return response(400, "INVALID_INPUT", "缺少必要参数或参数类型无效");
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<ApiResponse<Void>> duplicate() {
    return response(409, "CONFLICT", "该记录已存在，请刷新后重试");
  }

  @ExceptionHandler({CannotAcquireLockException.class, QueryTimeoutException.class})
  public ResponseEntity<ApiResponse<Void>> busy() {
    return response(503, "DATABASE_BUSY", "服务繁忙，请稍后重试");
  }

  @ExceptionHandler(cn.shiguang.notes.common.exception.MediaStoreException.class)
  public ResponseEntity<ApiResponse<Void>> storage() {
    return response(503, "MEDIA_STORAGE_UNAVAILABLE", "图片存储暂时不可用，请稍后重试");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
    LOG.error("Unhandled request failure type={}", e.getClass().getSimpleName(), e);
    return response(500, "INTERNAL_ERROR", "处理失败，请根据追踪编号联系管理员");
  }

  private ResponseEntity<ApiResponse<Void>> response(int status, String code, String message) {
    return ResponseEntity.status(status)
        .body(new ApiResponse<>(code, message, null, MDC.get("traceId")));
  }
}
