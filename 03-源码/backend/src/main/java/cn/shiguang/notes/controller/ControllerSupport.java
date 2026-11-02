package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.security.RequestSecurityFilter;
import cn.shiguang.notes.vo.EmptyVO;
import jakarta.servlet.http.HttpServletRequest;

/** Common transport mechanics only; business decisions stay in services. */
abstract class ControllerSupport {

  protected Actor actor(HttpServletRequest request) {
    return RequestSecurityFilter.actor(request);
  }

  protected String key(HttpServletRequest request) {
    return request.getHeader("Idempotency-Key");
  }

  protected ApiResponse<EmptyVO> empty() {
    return ApiResponse.ok(new EmptyVO());
  }
}
