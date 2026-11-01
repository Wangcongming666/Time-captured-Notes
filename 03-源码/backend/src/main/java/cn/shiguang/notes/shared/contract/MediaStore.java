package cn.shiguang.notes.shared.contract;

import java.io.*;

/** Object keys are database-owned filenames, never public URLs or client paths. */
public interface MediaStore {
  void put(String key, byte[] bytes, String contentType) throws IOException;

  InputStream open(String key) throws IOException;

  void delete(String key) throws IOException;

  static void validateKey(String key) {
    if (key == null || !key.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,179}"))
      throw new IllegalArgumentException("Invalid media key");
  }
}
