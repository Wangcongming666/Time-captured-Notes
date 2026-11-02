package cn.shiguang.notes.storage;

import cn.shiguang.notes.common.contract.MediaStore;
import cn.shiguang.notes.common.exception.MediaStoreException;
import java.io.*;
import java.nio.file.*;

public class LocalMediaStore implements MediaStore {
  private final Path root;

  public LocalMediaStore(String root) throws IOException {
    this.root = Path.of(root).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  private Path path(String key) {
    MediaStore.validateKey(key);
    return root.resolve(key);
  }

  public void put(String key, byte[] bytes, String contentType) throws IOException {
    try (var input = new ByteArrayInputStream(bytes)) {
      put(key, input, bytes.length, contentType);
    }
  }

  public void put(String key, InputStream input, long size, String contentType) throws IOException {
    Path target = path(key), temp = null;
    try {
      temp = Files.createTempFile(root, "upload-", ".tmp");
      Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
      if (Files.size(temp) != size) throw new IOException("Media length mismatch");
      Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new MediaStoreException("write", e);
    } finally {
      if (temp != null) Files.deleteIfExists(temp);
    }
  }

  public InputStream open(String key) throws IOException {
    try {
      return Files.newInputStream(path(key));
    } catch (NoSuchFileException e) {
      throw new FileNotFoundException(key);
    } catch (IOException e) {
      throw new MediaStoreException("read", e);
    }
  }

  public void delete(String key) throws IOException {
    try {
      Files.deleteIfExists(path(key));
    } catch (IOException e) {
      throw new MediaStoreException("delete", e);
    }
  }
}
