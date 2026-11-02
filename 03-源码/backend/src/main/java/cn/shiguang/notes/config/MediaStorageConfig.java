package cn.shiguang.notes.config;

import cn.shiguang.notes.common.contract.MediaStore;
import cn.shiguang.notes.storage.LocalMediaStore;
import cn.shiguang.notes.storage.MinioMediaStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
public class MediaStorageConfig {
  @Bean
  static org.springframework.beans.factory.config.BeanFactoryPostProcessor productionMediaGuard(
      org.springframework.core.env.Environment env) {
    return factory -> {
      if (env.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"))
          && !"minio".equals(env.getProperty("app.media.storage", "local")))
        throw new IllegalStateException(
            "Production requires MinIO media storage; local override is not allowed");
    };
  }

  @Bean
  @ConditionalOnProperty(name = "app.media.storage", havingValue = "local", matchIfMissing = true)
  MediaStore localMediaStore(@Value("${app.media-root}") String root) throws java.io.IOException {
    return new LocalMediaStore(root);
  }

  @Bean
  @ConditionalOnProperty(name = "app.media.storage", havingValue = "minio")
  MediaStore minioMediaStore(
      @Value("${app.media.minio.endpoint:}") String endpoint,
      @Value("${app.media.minio.access-key:}") String accessKey,
      @Value("${app.media.minio.secret-key:}") String secretKey,
      @Value("${app.media.minio.bucket:shiguang-media}") String bucket,
      @Value("${app.media.minio.create-bucket:false}") boolean createBucket,
      ObjectMapper json) {
    if (endpoint.isBlank() || accessKey.isBlank() || secretKey.isBlank() || bucket.isBlank())
      throw new IllegalStateException(
          "MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY and MINIO_BUCKET are required");
    var http =
        new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(60))
            .callTimeout(Duration.ofSeconds(90))
            .retryOnConnectionFailure(false)
            .build();
    var client =
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .httpClient(http)
            .build();
    try {
      return new MinioMediaStore(client, bucket, createBucket, json);
    } catch (RuntimeException failure) {
      try {
        client.close();
      } catch (Exception ignored) {
      }
      throw failure;
    }
  }
}
