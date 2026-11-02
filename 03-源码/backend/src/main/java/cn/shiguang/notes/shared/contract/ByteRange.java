package cn.shiguang.notes.shared.contract;

import cn.shiguang.notes.shared.error.BizException;

/** A single inclusive HTTP byte range. Multiple ranges are deliberately unsupported. */
public record ByteRange(long start, long end) {
  public long length() {
    return end - start + 1;
  }

  public static ByteRange parse(String value, long size) {
    try {
      if (size <= 0 || !value.matches("bytes=[0-9]*-[0-9]*")) throw new IllegalArgumentException();
      String[] parts = value.substring(6).split("-", -1);
      long start, end;
      if (parts[0].isEmpty()) {
        long suffix = Long.parseLong(parts[1]);
        if (suffix <= 0) throw new IllegalArgumentException();
        start = Math.max(0, size - suffix);
        end = size - 1;
      } else {
        start = Long.parseLong(parts[0]);
        end = parts[1].isEmpty() ? size - 1 : Math.min(Long.parseLong(parts[1]), size - 1);
      }
      if (start >= size || start > end) throw new IllegalArgumentException();
      return new ByteRange(start, end);
    } catch (RuntimeException e) {
      throw new BizException(416, "INVALID_RANGE", "请求的媒体范围无效");
    }
  }
}
