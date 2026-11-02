package cn.shiguang.notes.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMediaStoreTest {
  @TempDir Path root;

  @Test
  void preservesBytesAndDeletesIdempotently() throws Exception {
    var store = new LocalMediaStore(root.toString());
    store.put("test.png", new byte[] {1, 2, 3}, "image/png");
    try (var in = store.open("test.png")) {
      assertArrayEquals(new byte[] {1, 2, 3}, in.readAllBytes());
    }
    store.delete("test.png");
    store.delete("test.png");
    assertThrows(FileNotFoundException.class, () -> store.open("test.png"));
  }

  @Test
  void rejectsTraversalAndAbsolutePaths() throws Exception {
    var store = new LocalMediaStore(root.toString());
    for (String key : new String[] {"../private.png", "/private.png", "C:\\private.png", "a/b.png"})
      assertThrows(
          IllegalArgumentException.class, () -> store.put(key, new byte[] {1}, "image/png"));
  }
}
