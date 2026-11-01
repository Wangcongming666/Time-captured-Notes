package cn.shiguang.notes.platform.security;

import cn.shiguang.notes.shared.contract.Actor;
import cn.shiguang.notes.shared.contract.Tokens;
import cn.shiguang.notes.shared.error.BizException;
import cn.shiguang.notes.workflows.identity.IdentityUseCases;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Database-authoritative authentication and explicit synchronized CSRF for all mutations. */
@Component
@Order(1)
public class RequestSecurityFilter extends OncePerRequestFilter {
  public static final String ACTOR = "shiguang.actor";
  private final IdentityUseCases identity;
  private final ObjectMapper json;
  private final String key;
  private final RateGuard limits;
  private final boolean secure;

  public RequestSecurityFilter(
      IdentityUseCases identity,
      ObjectMapper json,
      RateGuard limits,
      @Value("${app.secure-cookie}") boolean secure,
      @Value("${app.cursor-key}") String key) {
    this.identity = identity;
    this.json = json;
    this.key = key;
    this.limits = limits;
    this.secure = secure;
    if (key.length() < 32)
      throw new IllegalArgumentException("CURSOR_HMAC_KEY must contain at least 32 characters");
  }

  public static String cookie(HttpServletRequest request, String name) {
    if (request.getCookies() != null)
      for (Cookie c : request.getCookies()) if (c.getName().equals(name)) return c.getValue();
    return null;
  }

  public static Actor actor(HttpServletRequest r) {
    return (Actor) r.getAttribute(ACTOR);
  }

  public String anonymousToken() {
    String value = Instant.now().getEpochSecond() + "." + Tokens.random();
    return value + "." + Tokens.sign(value, key);
  }

  public String anonymousCsrf(String cookie) {
    return Tokens.sign(cookie, key);
  }

  private boolean validAnonymous(String value, String csrf) {
    try {
      String[] p = value.split("\\.");
      String raw = p[0] + "." + p[1];
      long age = Instant.now().getEpochSecond() - Long.parseLong(p[0]);
      return p.length == 3
          && age >= 0
          && age < 600
          && Tokens.same(p[2], Tokens.sign(raw, key))
          && Tokens.same(csrf, anonymousCsrf(value));
    } catch (Exception e) {
      return false;
    }
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String trace = UUID.randomUUID().toString();
    MDC.put("traceId", trace);
    res.setHeader("X-Trace-Id", trace);
    try {
      String path = req.getRequestURI();
      if (!path.startsWith("/api/")) {
        chain.doFilter(req, res);
        return;
      }
      boolean read = Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod());
      limits.check(
          "ip:"
              + req.getRemoteAddr()
              + ":"
              + (read ? "read" : path.startsWith("/api/v1/auth/") ? "auth" : "write"),
          read ? 120 : path.startsWith("/api/v1/auth/") ? 20 : 60);
      String token = cookie(req, "SG_SESSION");
      Actor actor = identity.authenticate(token);
      req.setAttribute(ACTOR, actor);
      if (actor.id() > 0 && !read)
        limits.check(
            "user:" + actor.id() + ":" + (path.equals("/api/v1/media") ? "upload" : "write"),
            path.equals("/api/v1/media") ? 10 : 30);
      String visitor = cookie(req, "SG_VISITOR");
      if (visitor == null || !visitor.matches("[a-zA-Z0-9_-]{20,100}")) {
        visitor = Tokens.random();
        res.addHeader(
            "Set-Cookie",
            org.springframework.http.ResponseCookie.from("SG_VISITOR", visitor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(2592000)
                .build()
                .toString());
      }
      req.setAttribute("shiguang.visitor", visitor);
      boolean authPath =
          path.equals("/api/v1/auth/csrf")
              || path.equals("/api/v1/auth/login")
              || path.equals("/api/v1/auth/register");
      boolean publicRead =
          "GET".equals(req.getMethod())
              && (path.matches("/api/v1/(feed|search|topics)")
                  || path.matches("/api/v1/notes/[0-9]+(/comments|/feedback)?")
                  || path.matches("/api/v1/users/[0-9]+(/following|/followers)?")
                  || path.matches("/api/v1/comments/[0-9]+/replies")
                  || path.matches("/api/v1/media/[0-9]+/content"));
      if (!authPath && !publicRead) actor.requireLogin();
      if (!Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod())) {
        String csrf = req.getHeader("X-CSRF-TOKEN");
        boolean valid =
            actor.id() > 0
                ? identity.checkCsrf(token, csrf)
                : authPath && validAnonymous(cookie(req, "SG_ANON"), csrf);
        if (!valid) throw new BizException(403, "CSRF_INVALID", "安全令牌已失效，请刷新后重试");
      }
      chain.doFilter(req, res);
      if (actor.id() > 0 && res.getStatus() < 400)
        try {
          identity.active(actor);
        } catch (org.springframework.dao.DataAccessException unavailable) {
          org.slf4j.LoggerFactory.getLogger(RequestSecurityFilter.class)
              .warn("Activity aggregation unavailable trace={}", trace);
        }
    } catch (BizException e) {
      if (e.status() == 429) res.setHeader("Retry-After", "60");
      error(res, e.status(), e.code(), e.getMessage(), trace);
    } catch (org.springframework.dao.DataAccessException e) {
      error(res, 503, "SERVICE_UNAVAILABLE", "服务暂时不可用，请稍后重试", trace);
    } finally {
      MDC.clear();
    }
  }

  private void error(HttpServletResponse res, int status, String code, String message, String trace)
      throws IOException {
    if (res.isCommitted()) return;
    res.setStatus(status);
    res.setContentType("application/json;charset=UTF-8");
    var body = new LinkedHashMap<String, Object>();
    body.put("code", code);
    body.put("message", message);
    body.put("data", null);
    body.put("traceId", trace);
    json.writeValue(res.getWriter(), body);
  }
}
