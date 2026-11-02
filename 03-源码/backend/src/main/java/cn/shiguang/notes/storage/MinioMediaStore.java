package cn.shiguang.notes.storage;

import cn.shiguang.notes.common.contract.MediaStore;
import cn.shiguang.notes.common.exception.MediaStoreException;
import com.fasterxml.jackson.databind.*;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import java.io.*;

/** Streams private objects through the authorized API; never publishes bucket policies or URLs. */
public class MinioMediaStore implements MediaStore, AutoCloseable {
  private final MinioClient client;
  private final String bucket;

  @Override
  public void close() throws Exception {
    client.close();
  }

  public MinioMediaStore(
      MinioClient client, String bucket, boolean createBucket, ObjectMapper json) {
    this.client = client;
    this.bucket = bucket;
    try {
      if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
        if (!createBucket)
          throw new IllegalStateException("MinIO bucket must be provisioned before startup");
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      }
      try {
        String raw = client.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucket).build());
        if (raw != null && !raw.isBlank()) {
          JsonNode statements = json.readTree(raw).path("Statement");
          if (statements.isObject()) checkStatement(statements);
          else if (statements.isArray())
            for (JsonNode statement : statements) checkStatement(statement);
          else throw new IllegalStateException("Cannot validate MinIO bucket policy");
        }
      } catch (ErrorResponseException e) {
        if (!"NoSuchBucketPolicy".equals(e.errorResponse().code())) throw e;
      }
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Cannot initialize private MinIO media bucket; check endpoint, credentials and"
              + " permissions");
    }
  }

  private static boolean anonymous(JsonNode node) {
    if (node.isTextual()) return "*".equals(node.asText());
    for (JsonNode child : node) if (anonymous(child)) return true;
    return false;
  }

  private static void checkStatement(JsonNode statement) {
    if ("Allow".equals(statement.path("Effect").asText()) && anonymous(statement.path("Principal")))
      throw new IllegalStateException("MinIO media bucket must not allow anonymous access");
  }

  public void put(String key, byte[] bytes, String contentType) throws IOException {
    try (var input = new ByteArrayInputStream(bytes)) {
      put(key, input, bytes.length, contentType);
    }
  }

  public void put(String key, InputStream in, long size, String contentType) throws IOException {
    MediaStore.validateKey(key);
    try {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(key).stream(in, size, -1)
              .contentType(contentType)
              .build());
    } catch (Exception e) {
      throw new MediaStoreException("write", e);
    }
  }

  public InputStream open(String key) throws IOException {
    return get(key, null, null);
  }

  public InputStream openRange(String key, long offset, long length) throws IOException {
    return get(key, offset, length);
  }

  private InputStream get(String key, Long offset, Long length) throws IOException {
    MediaStore.validateKey(key);
    try {
      var args = GetObjectArgs.builder().bucket(bucket).object(key);
      if (offset != null) args.offset(offset).length(length);
      return client.getObject(args.build());
    } catch (ErrorResponseException e) {
      if (java.util.Set.of("NoSuchKey", "NoSuchObject").contains(e.errorResponse().code()))
        throw new FileNotFoundException(key);
      throw new MediaStoreException("read", e);
    } catch (Exception e) {
      throw new MediaStoreException("read", e);
    }
  }

  public void delete(String key) throws IOException {
    MediaStore.validateKey(key);
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    } catch (Exception e) {
      throw new MediaStoreException("delete", e);
    }
  }
}
