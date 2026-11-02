package cn.shiguang.notes.validation;

import static org.assertj.core.api.Assertions.*;

import cn.shiguang.notes.common.exception.BizException;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoNoteRulesTest {
  @Test
  void acceptsEitherVideoOrImages() {
    MediaRules.validateNote(List.of("VIDEO"));
    MediaRules.validateNote(List.of("IMAGE", "IMAGE"));
  }

  @Test
  void rejectsMixedAndMultipleVideos() {
    assertThatThrownBy(() -> MediaRules.validateNote(List.of("VIDEO", "IMAGE")))
        .isInstanceOf(BizException.class);
    assertThatThrownBy(() -> MediaRules.validateNote(List.of("VIDEO", "VIDEO")))
        .isInstanceOf(BizException.class);
  }
}
