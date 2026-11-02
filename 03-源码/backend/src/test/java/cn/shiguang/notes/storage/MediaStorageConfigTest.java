package cn.shiguang.notes.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.config.MediaStorageConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MediaStorageConfigTest {
  @org.junit.jupiter.api.io.TempDir java.nio.file.Path root;

  @Test
  void productionRejectsCanonicalLocalOverride() {
    new ApplicationContextRunner()
        .withInitializer(
            new org.springframework.boot.test.context.ConfigDataApplicationContextInitializer())
        .withUserConfiguration(MediaStorageConfig.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withPropertyValues(
            "spring.profiles.active=prod",
            "app.media.storage=local",
            "app.media-root=" + root.resolve("must-not-store-locally"))
        .run(
            c -> {
              assertThat(c).hasFailed();
              assertThat(c.getStartupFailure()).hasMessageContaining("Production requires MinIO");
            });
  }

  @Test
  void missingProductionCredentialsFailsInsteadOfFallingBack() {
    new ApplicationContextRunner()
        .withUserConfiguration(MediaStorageConfig.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withPropertyValues("app.media.storage=minio")
        .run(
            c -> {
              assertThat(c).hasFailed();
              assertThat(c.getStartupFailure())
                  .hasRootCauseMessage(
                      "MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY and MINIO_BUCKET are"
                          + " required");
            });
  }

  @Test
  void productionProfileOverridesLocalStorageChoice() {
    new ApplicationContextRunner()
        .withInitializer(
            new org.springframework.boot.test.context.ConfigDataApplicationContextInitializer())
        .withUserConfiguration(MediaStorageConfig.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withPropertyValues("spring.profiles.active=prod", "MEDIA_STORAGE=local")
        .run(
            c -> {
              assertThat(c).hasFailed();
              assertThat(c.getStartupFailure())
                  .hasRootCauseMessage(
                      "MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY and MINIO_BUCKET are"
                          + " required");
            });
  }

  @Test
  void singleStatementAnonymousPolicyIsRejected() throws Exception {
    var sdk = mock(MinioClient.class);
    when(sdk.bucketExists(any())).thenReturn(true);
    when(sdk.getBucketPolicy(any()))
        .thenReturn(
            "{\"Statement\":{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":\"s3:GetObject\"}}");
    assertThrows(
        IllegalStateException.class,
        () -> new MinioMediaStore(sdk, "test-bucket", false, new ObjectMapper()));
  }
}
