package cn.shiguang.notes.platform.storage;

import cn.shiguang.notes.shared.error.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Fixed local-only subprocess arguments; files are bounded before probing. No shell is used. */
@Component
public class VideoProcessor {
  private final ObjectMapper json;
  private final String ffmpeg, ffprobe;
  private final int maxSeconds;
  private final long maxBytes;

  public VideoProcessor(
      ObjectMapper json,
      @Value("${app.media.video.ffmpeg:ffmpeg}") String ffmpeg,
      @Value("${app.media.video.ffprobe:ffprobe}") String ffprobe,
      @Value("${app.media.video.max-seconds:300}") int maxSeconds,
      @Value("${app.media.video.max-bytes:104857600}") long maxBytes) {
    this.json = json;
    this.ffmpeg = ffmpeg;
    this.ffprobe = ffprobe;
    this.maxSeconds = maxSeconds;
    this.maxBytes = maxBytes;
  }

  public long maxBytes() {
    return maxBytes;
  }

  public int maxSeconds() {
    return maxSeconds;
  }

  public record Result(Path video, Path poster, int width, int height, long durationMs) {}

  public Result process(Path dir) throws IOException {
    Path source = dir.resolve("input.mp4"),
        video = dir.resolve("video.mp4"),
        poster = dir.resolve("poster.jpg");
    if (Files.size(source) > maxBytes) throw new BizException(413, "FILE_TOO_LARGE", "视频超过允许的大小");
    run(
        dir,
        ffprobe,
        "-v",
        "error",
        "-protocol_whitelist",
        "file",
        "-enable_drefs",
        "0",
        "-f",
        "mov",
        "-show_entries",
        "stream=codec_type,codec_name,width,height:format=duration",
        "-of",
        "json",
        source.toString());
    var data = json.readTree(Files.readString(dir.resolve("stdout.log")));
    var streams = data.path("streams");
    int width = 0, height = 0, videos = 0;
    for (var stream : streams) {
      String type = stream.path("codec_type").asText(), codec = stream.path("codec_name").asText();
      if (type.equals("video")) {
        videos++;
        width = stream.path("width").asInt();
        height = stream.path("height").asInt();
        if (!codec.equals("h264")) throw unsupported();
      } else if (type.equals("audio") && !codec.equals("aac")) throw unsupported();
    }
    double duration = data.path("format").path("duration").asDouble(0);
    if (videos != 1
        || width <= 0
        || height <= 0
        || width > 4096
        || height > 4096
        || !Double.isFinite(duration)
        || duration <= 0) throw unsupported();
    if (duration > maxSeconds) throw BizException.invalid("视频时长不能超过" + maxSeconds + "秒");
    run(
        dir,
        ffmpeg,
        "-v",
        "error",
        "-nostdin",
        "-y",
        "-protocol_whitelist",
        "file",
        "-enable_drefs",
        "0",
        "-f",
        "mov",
        "-i",
        source.toString(),
        "-map",
        "0:v:0",
        "-map",
        "0:a:0?",
        "-c",
        "copy",
        "-map_metadata",
        "-1",
        "-movflags",
        "+faststart",
        video.toString());
    run(
        dir,
        ffmpeg,
        "-v",
        "error",
        "-nostdin",
        "-y",
        "-protocol_whitelist",
        "file",
        "-i",
        video.toString(),
        "-frames:v",
        "1",
        "-vf",
        "scale=1280:1280:force_original_aspect_ratio=decrease",
        "-update",
        "1",
        poster.toString());
    if (Files.size(video) > maxBytes) throw new BizException(413, "FILE_TOO_LARGE", "处理后视频超过允许的大小");
    return new Result(video, poster, width, height, Math.round(duration * 1000));
  }

  private BizException unsupported() {
    return new BizException(415, "UNSUPPORTED_VIDEO", "请上传MP4视频（H.264画面、AAC音频），并检查文件是否完整");
  }

  private void run(Path dir, String... args) throws IOException {
    Process process;
    try {
      var builder =
          new ProcessBuilder(args)
              .redirectOutput(dir.resolve("stdout.log").toFile())
              .redirectError(dir.resolve("stderr.log").toFile());
      // Do not pass application credentials to media subprocesses.
      var inherited = new HashMap<>(builder.environment());
      builder.environment().clear();
      for (var key : List.of("PATH", "Path", "SystemRoot", "WINDIR", "TEMP", "TMP"))
        if (inherited.containsKey(key)) builder.environment().put(key, inherited.get(key));
      process = builder.start();
    } catch (IOException e) {
      throw new BizException(503, "VIDEO_PROCESSOR_UNAVAILABLE", "视频处理服务暂不可用");
    }
    try {
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        process.waitFor(5, TimeUnit.SECONDS);
        throw new BizException(422, "VIDEO_TIMEOUT", "视频处理超时，请尝试更短的视频");
      }
      if (process.exitValue() != 0) throw unsupported();
    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new IOException("Video processing interrupted", e);
    }
  }
}
