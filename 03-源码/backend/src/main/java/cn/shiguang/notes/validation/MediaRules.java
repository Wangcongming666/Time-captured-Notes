package cn.shiguang.notes.validation;

import cn.shiguang.notes.common.exception.BizException;
import java.util.List;

public final class MediaRules {
  private MediaRules() {}

  public static void validateNote(List<String> types) {
    if (types.stream().anyMatch("VIDEO"::equals)
        && (types.size() != 1 || !"VIDEO".equals(types.getFirst())))
      throw BizException.invalid("请选择一段视频或最多9张图片，不能混合发布");
  }
}
