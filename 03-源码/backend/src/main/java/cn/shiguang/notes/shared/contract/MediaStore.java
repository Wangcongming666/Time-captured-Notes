package cn.shiguang.notes.shared.contract;

import java.io.*;

/** Object keys are database-owned filenames, never public URLs or client paths. */
public interface MediaStore {
  void put(String key, byte[] bytes, String contentType) throws IOException;

  default void put(String key, InputStream input, long size, String contentType)
      throws IOException {
    throw new IOException("Streaming uploads are not supported by this media store");
  }

  default InputStream openRange(String key, long offset, long length) throws IOException {
    InputStream input = open(key);
    try {
      input.skipNBytes(offset);
    } catch (IOException e) {
      input.close();
      throw e;
    }
    return new FilterInputStream(input) {
      long remaining = length;

      public int read() throws IOException {
        if (remaining == 0) return -1;
        int b = super.read();
        if (b >= 0) remaining--;
        return b;
      }

      public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) return 0;
        if (remaining == 0) return -1;
        int n = in.read(b, off, (int) Math.min(len, remaining));
        if (n > 0) remaining -= n;
        return n;
      }
    };
  }

  InputStream open(String key) throws IOException;

  void delete(String key) throws IOException;

  static void validateKey(String key) {
    if (key == null || !key.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,179}"))
      throw new IllegalArgumentException("Invalid media key");
  }
}
