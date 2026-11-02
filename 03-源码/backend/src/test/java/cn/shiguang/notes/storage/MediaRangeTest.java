package cn.shiguang.notes.storage;

import static org.assertj.core.api.Assertions.*;

import cn.shiguang.notes.shared.contract.ByteRange;
import cn.shiguang.notes.shared.error.BizException;
import org.junit.jupiter.api.Test;

class MediaRangeTest {
  @Test
  void acceptsClosedOpenAndSuffixRanges() {
    assertThat(ByteRange.parse("bytes=2-5", 10)).isEqualTo(new ByteRange(2, 5));
    assertThat(ByteRange.parse("bytes=8-", 10)).isEqualTo(new ByteRange(8, 9));
    assertThat(ByteRange.parse("bytes=-3", 10)).isEqualTo(new ByteRange(7, 9));
    assertThat(ByteRange.parse("bytes=0-999", 10)).isEqualTo(new ByteRange(0, 9));
  }

  @Test
  void rejectsMalformedAndUnsatisfiableRanges() {
    for (String input :
        new String[] {
          "bytes=20-",
          "bytes=5-2",
          "bytes=-0",
          "bytes=-",
          "bytes=0-1,3-4",
          "bytes=999999999999999999999-"
        })
      assertThatThrownBy(() -> ByteRange.parse(input, 10))
          .isInstanceOfSatisfying(BizException.class, e -> assertThat(e.status()).isEqualTo(416));
  }
}
