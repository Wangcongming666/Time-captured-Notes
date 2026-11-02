package cn.shiguang.notes.validation;

import cn.shiguang.notes.common.exception.BizException;

/** Framework-independent note lifecycle and Unicode boundaries. */
public final class NoteRules {
  private NoteRules() {}

  public static boolean isPublic(String status) {
    return "PUBLISHED".equals(status);
  }

  public static void validateSubmission(String title, String body, int media, int topics) {
    length(title, 1, 40, "标题");
    length(body, 1, 2000, "正文");
    if (media < 1 || media > 9) throw BizException.invalid("请选择1–9张图片");
    if (topics < 1 || topics > 5) throw BizException.invalid("请选择1–5个有效话题");
  }

  public static String afterEdit(String status) {
    return switch (status) {
      case "DRAFT", "REJECTED", "OFFLINE" -> "DRAFT";
      case "PUBLISHED" -> "PENDING";
      default -> throw BizException.conflict("待审核内容请先撤回；删除内容不能恢复");
    };
  }

  public static void length(String value, int min, int max, String field) {
    int n = value == null ? 0 : value.codePointCount(0, value.length());
    if (n < min || n > max || (min > 0 && value.isBlank()))
      throw BizException.invalid(field + "长度须为" + min + "–" + max + "字");
  }
}
