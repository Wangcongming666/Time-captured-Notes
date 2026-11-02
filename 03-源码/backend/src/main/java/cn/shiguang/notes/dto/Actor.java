package cn.shiguang.notes.dto;

import cn.shiguang.notes.common.exception.BizException;
import java.util.Set;

/** Authenticated request identity; guest id is zero. */
public record Actor(long id, Set<String> roles) {
  public static Actor guest() {
    return new Actor(0, Set.of());
  }

  public void requireLogin() {
    if (id == 0) throw new BizException(401, "UNAUTHENTICATED", "请先登录");
  }

  public void require(String role) {
    requireLogin();
    if (!roles.contains(role)) throw BizException.forbidden();
  }

  public boolean has(String role) {
    return roles.contains(role);
  }
}
