package cn.shiguang.notes.storage;

import static org.assertj.core.api.Assertions.*;

import cn.shiguang.notes.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoProcessorTest {
  @TempDir Path dir;

  @Test
  void probesRealMp4AndCreatesPlayableVideoAndPoster() throws Exception {
    var process =
        new ProcessBuilder(
                "ffmpeg",
                "-v",
                "error",
                "-nostdin",
                "-y",
                "-f",
                "lavfi",
                "-i",
                "color=c=red:s=160x240:d=1",
                "-c:v",
                "libx264",
                "-pix_fmt",
                "yuv420p",
                dir.resolve("input.mp4").toString())
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    assertThat(process.waitFor(30, TimeUnit.SECONDS)).isTrue();
    assertThat(process.exitValue()).isZero();
    var result =
        new VideoProcessor(new ObjectMapper(), "ffmpeg", "ffprobe", 300, 104857600).process(dir);
    assertThat(result.width()).isEqualTo(160);
    assertThat(result.height()).isEqualTo(240);
    assertThat(result.durationMs()).isBetween(900L, 1100L);
    assertThat(Files.size(result.video())).isPositive();
    assertThat(javax.imageio.ImageIO.read(result.poster().toFile())).isNotNull();
  }

  @Test
  void rejectsFakeMp4() throws Exception {
    Files.writeString(dir.resolve("input.mp4"), "not a video");
    assertThatThrownBy(
            () ->
                new VideoProcessor(new ObjectMapper(), "ffmpeg", "ffprobe", 300, 104857600)
                    .process(dir))
        .isInstanceOfSatisfying(BizException.class, e -> assertThat(e.status()).isEqualTo(415));
  }

  @Test
  void enforcesConfiguredDurationAndSize() throws Exception {
    var process =
        new ProcessBuilder(
                "ffmpeg",
                "-v",
                "error",
                "-nostdin",
                "-y",
                "-f",
                "lavfi",
                "-i",
                "color=c=red:s=160x240:d=2",
                "-c:v",
                "libx264",
                "-pix_fmt",
                "yuv420p",
                dir.resolve("input.mp4").toString())
            .start();
    assertThat(process.waitFor(30, TimeUnit.SECONDS)).isTrue();
    assertThat(process.exitValue()).isZero();
    assertThatThrownBy(
            () ->
                new VideoProcessor(new ObjectMapper(), "ffmpeg", "ffprobe", 1, 104857600)
                    .process(dir))
        .isInstanceOfSatisfying(BizException.class, e -> assertThat(e.getMessage()).contains("时长"));
    assertThatThrownBy(
            () -> new VideoProcessor(new ObjectMapper(), "ffmpeg", "ffprobe", 300, 1).process(dir))
        .isInstanceOfSatisfying(BizException.class, e -> assertThat(e.status()).isEqualTo(413));
  }

  @Test
  void missingProcessorReturnsRecoverableServiceError() throws Exception {
    Files.writeString(dir.resolve("input.mp4"), "test");
    assertThatThrownBy(
            () ->
                new VideoProcessor(
                        new ObjectMapper(),
                        "not-an-installed-ffmpeg",
                        "not-an-installed-ffprobe",
                        300,
                        104857600)
                    .process(dir))
        .isInstanceOfSatisfying(BizException.class, e -> assertThat(e.status()).isEqualTo(503));
  }
}
