package cn.shiguang.notes.integration;

import static org.junit.jupiter.api.Assertions.*;

import io.minio.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

/** Repeats the actual HTTP lifecycle against a private, real MinIO server. */
class MinioApiFlowIT extends ApiFlowIT {
  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("shiguang-minio-test:2025-10-15")
          .withEnv("MINIO_ROOT_USER", "storage_test")
          .withEnv("MINIO_ROOT_PASSWORD", "MinioIntegrationPass123!")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  static String endpoint() {
    return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
  }

  @DynamicPropertySource
  static void minioProperties(DynamicPropertyRegistry r) {
    r.add("app.media.storage", () -> "minio");
    r.add("app.media.minio.endpoint", MinioApiFlowIT::endpoint);
    r.add("app.media.minio.access-key", () -> "storage_test");
    r.add("app.media.minio.secret-key", () -> "MinioIntegrationPass123!");
    r.add("app.media.minio.bucket", () -> "shiguang-test");
    r.add("app.media.minio.create-bucket", () -> true);
    r.add("app.media-root", () -> "target/minio-unused-local-media");
  }

  @Test
  @Order(17)
  void objectsStayPrivateAndNeverCreateALocalStorageDirectory() throws Exception {
    assertInstanceOf(cn.shiguang.notes.storage.MinioMediaStore.class, mediaStore);
    var media =
        database.one(
            "SELECT m.id,m.storage_key FROM t_media m JOIN t_note_media nm ON nm.media_id=m.id JOIN"
                + " t_note n ON n.id=nm.note_id WHERE n.status='PUBLISHED' ORDER BY m.id LIMIT 1");
    String key = media.get("storageKey").toString();
    var throughApi =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create(
                            "http://localhost:"
                                + port
                                + "/api/v1/media/"
                                + media.get("id")
                                + "/content"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, throughApi.statusCode());
    assertTrue(throughApi.body().length > 100);
    var response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(endpoint() + "/shiguang-test/" + key))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.discarding());
    assertEquals(403, response.statusCode());
    assertFalse(Files.exists(Path.of(mediaRoot)));
  }

  @Test
  @Order(18)
  void refusesAnAnonymousReadBucket() throws Exception {
    var sdk =
        MinioClient.builder()
            .endpoint(endpoint())
            .credentials("storage_test", "MinioIntegrationPass123!")
            .build();
    String bucket = "public-negative-test";
    sdk.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    sdk.setBucketPolicy(
        SetBucketPolicyArgs.builder()
            .bucket(bucket)
            .config(
                "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::public-negative-test/*\"]}]}")
            .build());
    assertThrows(
        IllegalStateException.class,
        () -> new cn.shiguang.notes.storage.MinioMediaStore(sdk, bucket, false, json));
  }
}
