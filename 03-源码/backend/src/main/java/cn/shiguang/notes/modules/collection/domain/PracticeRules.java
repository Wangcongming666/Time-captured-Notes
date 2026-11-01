package cn.shiguang.notes.modules.collection.domain;

import cn.shiguang.notes.shared.error.BizException;

/** Terminal practices cannot silently reopen or skip doing. */
public final class PracticeRules {
  private PracticeRules() {}

  public static void transition(String from, String to) {
    boolean allowed =
        ("PLANNED".equals(from) && ("DOING".equals(to) || "ABANDONED".equals(to)))
            || ("DOING".equals(from) && ("DONE".equals(to) || "ABANDONED".equals(to)));
    if (!allowed) throw BizException.conflict("不允许的实践状态转换");
  }
}
