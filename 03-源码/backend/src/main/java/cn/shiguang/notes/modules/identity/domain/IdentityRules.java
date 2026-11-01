package cn.shiguang.notes.modules.identity.domain;

import cn.shiguang.notes.shared.error.BizException;
import java.nio.charset.StandardCharsets;

/** Credential boundaries independent of the security framework. */
public final class IdentityRules {
  private IdentityRules() {}

  public static void validateRegistration(String username, String password, String nickname) {
    if (username == null || !username.matches("[a-z0-9_]{4,32}"))
      throw BizException.invalid("账号须为4–32位小写字母、数字或下划线");
    if (password == null
        || password.length() < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw BizException.invalid("密码至少8位，UTF-8编码不超过72字节");
    if (nickname == null
        || nickname.isBlank()
        || nickname.codePointCount(0, nickname.length()) > 40)
      throw BizException.invalid("昵称长度须为1–40字");
  }
}
