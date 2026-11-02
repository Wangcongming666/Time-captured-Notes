package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.ApiResponse;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.security.RequestSecurityFilter;
import cn.shiguang.notes.service.AccountService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Credential endpoints never serialize token fields into JSON. */
@RestController
@RequestMapping("/api/v1")
public class IdentityController {
  private final AccountService use;
  private final RequestSecurityFilter filter;
  private final boolean secure;
  private final cn.shiguang.notes.security.RateGuard limits;

  public IdentityController(
      AccountService use,
      RequestSecurityFilter filter,
      cn.shiguang.notes.security.RateGuard limits,
      @Value("${app.secure-cookie}") boolean secure) {
    this.use = use;
    this.filter = filter;
    this.secure = secure;
    this.limits = limits;
  }

  private void cookie(HttpServletResponse response, String name, String value, int seconds) {
    response.addHeader(
        "Set-Cookie",
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(seconds)
            .build()
            .toString());
  }

  @GetMapping("/auth/csrf")
  public ApiResponse<CsrfVO> csrf(HttpServletRequest req, HttpServletResponse res) {
    String token = RequestSecurityFilter.cookie(req, "SG_SESSION");
    if (RequestSecurityFilter.actor(req).id() > 0)
      return ApiResponse.ok(new CsrfVO(use.csrf(token)));
    String anon = filter.anonymousToken();
    cookie(res, "SG_ANON", anon, 600);
    return ApiResponse.ok(new CsrfVO(filter.anonymousCsrf(anon)));
  }

  @PostMapping("/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<IdVO> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok(use.register(request));
  }

  @PostMapping("/auth/login")
  public ApiResponse<UserVO> login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse res) {
    limits.check("account:" + request.username().toLowerCase(Locale.ROOT), 5);
    var session = use.login(request);
    String token = session.token();
    cookie(res, "SG_SESSION", token, 86400);
    cookie(res, "SG_ANON", "", 0);
    return ApiResponse.ok(session.user());
  }

  @PostMapping("/auth/logout")
  public ApiResponse<EmptyVO> logout(HttpServletRequest req, HttpServletResponse res) {
    use.logout(RequestSecurityFilter.cookie(req, "SG_SESSION"));
    cookie(res, "SG_SESSION", "", 0);
    return ApiResponse.ok(new EmptyVO());
  }

  @GetMapping("/me")
  public ApiResponse<UserVO> me(HttpServletRequest req) {
    return ApiResponse.ok(use.profile(RequestSecurityFilter.actor(req)));
  }

  @GetMapping("/users/{id}")
  public ApiResponse<UserVO> user(@PathVariable long id) {
    return ApiResponse.ok(use.publicProfile(id));
  }

  @GetMapping("/users/{id}/summary")
  public ApiResponse<ProfileSummaryVO> summary(@PathVariable long id, HttpServletRequest req) {
    return ApiResponse.ok(use.summary(RequestSecurityFilter.actor(req), id));
  }

  @PutMapping("/me/profile")
  public ApiResponse<UserVO> profile(
      @Valid @RequestBody ProfileRequest request, HttpServletRequest req) {
    return ApiResponse.ok(use.update(RequestSecurityFilter.actor(req), request));
  }

  @PutMapping("/admin/users/{id}/status")
  public ApiResponse<EmptyVO> status(
      @PathVariable long id,
      @Valid @RequestBody AccountStatusRequest request,
      HttpServletRequest req) {
    use.status(RequestSecurityFilter.actor(req), id, request);
    return ApiResponse.ok(new EmptyVO());
  }

  @PutMapping("/admin/users/{id}/roles")
  public ApiResponse<EmptyVO> roles(
      @PathVariable long id, @Valid @RequestBody RolesRequest request, HttpServletRequest req) {
    use.roles(RequestSecurityFilter.actor(req), id, request);
    return ApiResponse.ok(new EmptyVO());
  }
}
