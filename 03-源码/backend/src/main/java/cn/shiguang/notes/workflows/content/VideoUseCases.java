package cn.shiguang.notes.workflows.content;

import cn.shiguang.notes.modules.content.api.ContentApi;
import cn.shiguang.notes.platform.storage.VideoProcessor;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoUseCases {
  private final MediaStore store;
  private final ContentApi content;
  private final VideoProcessor processor;
  private final TransactionTemplate tx;
  private final Semaphore slots = new Semaphore(1);

  public VideoUseCases(
      MediaStore store, ContentApi content, VideoProcessor processor, TransactionTemplate tx) {
    this.store = store;
    this.content = content;
    this.processor = processor;
    this.tx = tx;
  }

  public Map<String, Object> capabilities() {
    return Data.map(
        "videoMaxBytes",
        processor.maxBytes(),
        "videoMaxSeconds",
        processor.maxSeconds(),
        "imageMaxBytes",
        10485760,
        "imageMaxCount",
        9,
        "videoFormat",
        "MP4 (H.264/AAC)");
  }

  public Map<String, Object> upload(Actor actor, MultipartFile file) throws IOException {
    actor.requireLogin();
    if (file.isEmpty() || file.getSize() > processor.maxBytes())
      throw new BizException(413, "FILE_TOO_LARGE", "视频为空或超过允许大小");
    if (!slots.tryAcquire()) throw new BizException(429, "VIDEO_BUSY", "视频处理繁忙，请稍后重试");
    Path dir = null;
    var registered = new java.util.concurrent.atomic.AtomicBoolean(false);
    String videoKey = UUID.randomUUID() + ".mp4", posterKey = UUID.randomUUID() + ".jpg";
    try {
      dir = Files.createTempDirectory("shiguang-video-");
      try (var input = file.getInputStream();
          var output = Files.newOutputStream(dir.resolve("input.mp4"))) {
        byte[] buffer = new byte[65536];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
          total += count;
          if (total > processor.maxBytes())
            throw new BizException(413, "FILE_TOO_LARGE", "视频超过允许大小");
          output.write(buffer, 0, count);
        }
      }
      var result = processor.process(dir);
      var digest = MessageDigest.getInstance("SHA-256");
      try (var input = new DigestInputStream(Files.newInputStream(result.video()), digest)) {
        input.transferTo(OutputStream.nullOutputStream());
      }
      long size = Files.size(result.video()), posterSize = Files.size(result.poster());
      try (var input = Files.newInputStream(result.video())) {
        store.put(videoKey, input, size, "video/mp4");
      }
      try (var input = Files.newInputStream(result.poster())) {
        store.put(posterKey, input, posterSize, "image/jpeg");
      }
      return tx.execute(
          status -> {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                  public void afterCompletion(int outcome) {
                    if (outcome == STATUS_ROLLED_BACK) {
                      remove(videoKey);
                      remove(posterKey);
                    } else if (outcome != STATUS_COMMITTED)
                      org.slf4j.LoggerFactory.getLogger(VideoUseCases.class)
                          .warn(
                              "Video commit outcome uncertain; retaining objects {} {}",
                              videoKey,
                              posterKey);
                  }
                });
            registered.set(true);
            return content.addVideo(
                actor,
                Data.map(
                    "storage_key",
                    videoKey,
                    "poster_storage_key",
                    posterKey,
                    "poster_size_bytes",
                    posterSize,
                    "media_type",
                    "VIDEO",
                    "purpose",
                    "NOTE",
                    "mime",
                    "video/mp4",
                    "size_bytes",
                    size,
                    "width",
                    result.width(),
                    "height",
                    result.height(),
                    "duration_ms",
                    result.durationMs(),
                    "sha256",
                    HexFormat.of().formatHex(digest.digest())));
          });
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    } catch (IOException | RuntimeException e) {
      if (!registered.get()) {
        remove(videoKey);
        remove(posterKey);
      }
      throw e;
    } finally {
      slots.release();
      if (dir != null) {
        try (var paths = Files.list(dir)) {
          for (Path path : paths.toList()) Files.deleteIfExists(path);
        } catch (IOException e) {
          org.slf4j.LoggerFactory.getLogger(VideoUseCases.class).warn("Video temp cleanup failed");
        }
        try {
          Files.deleteIfExists(dir);
        } catch (IOException e) {
          org.slf4j.LoggerFactory.getLogger(VideoUseCases.class)
              .warn("Video temp directory cleanup failed");
        }
      }
    }
  }

  private void remove(String key) {
    try {
      store.delete(key);
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(VideoUseCases.class)
          .warn("Video cleanup failed key={}", key);
    }
  }
}
