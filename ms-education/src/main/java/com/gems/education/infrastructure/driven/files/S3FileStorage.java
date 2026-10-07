package com.gems.education.infrastructure.driven.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores uploads in an S3 bucket while preserving the same opaque UUID keys as local storage. */
@Component
@ConditionalOnProperty(name = "files.storage", havingValue = "s3")
public class S3FileStorage implements FileStorage {
  private final S3AsyncClient client;
  private final String bucket;
  private final String prefix;

  public S3FileStorage(S3AsyncClient client,
                       @Value("${files.s3.bucket}") String bucket,
                       @Value("${files.s3.prefix:uploads}") String prefix) {
    if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("files.s3.bucket is required");
    this.client = client;
    this.bucket = bucket;
    this.prefix = prefix == null ? "" : prefix.replaceAll("^/+|/+$", "");
  }

  @Override
  public Mono<Long> write(String key, FilePart part) {
    String objectKey = objectKey(key);
    return Mono.usingWhen(
      Mono.fromCallable(() -> Files.createTempFile("gems-upload-", ".part"))
        .subscribeOn(Schedulers.boundedElastic()),
      temporary -> part.transferTo(temporary)
        .then(Mono.fromCallable(() -> Files.size(temporary)).subscribeOn(Schedulers.boundedElastic()))
        .flatMap(size -> Mono.fromFuture(client.putObject(
          PutObjectRequest.builder().bucket(bucket).key(objectKey).build(),
          AsyncRequestBody.fromFile(temporary)
        )).thenReturn(size)),
      this::removeTemporary
    );
  }

  @Override
  public Mono<Resource> read(String key) {
    var request = GetObjectRequest.builder().bucket(bucket).key(objectKey(key)).build();
    return Mono.fromFuture(client.getObject(request, AsyncResponseTransformer.toBytes()))
      .map(bytes -> (Resource) new ByteArrayResource(bytes.asByteArray()))
      .onErrorResume(this::isNotFound, error -> Mono.empty());
  }

  @Override
  public Mono<Void> delete(String key) {
    var request = DeleteObjectRequest.builder().bucket(bucket).key(objectKey(key)).build();
    return Mono.fromFuture(client.deleteObject(request)).then();
  }

  private String objectKey(String key) {
    if (!key.matches("[0-9a-f\\-]{36}")) throw new IllegalArgumentException("Invalid file key");
    return prefix.isBlank() ? key : prefix + "/" + key;
  }

  private Mono<Void> removeTemporary(Path path) {
    return Mono.fromRunnable(() -> {
      try {
        Files.deleteIfExists(path);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }).subscribeOn(Schedulers.boundedElastic()).then();
  }

  private boolean isNotFound(Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (current instanceof NoSuchKeyException) return true;
      if (current instanceof S3Exception s3 && s3.statusCode() == 404) return true;
      current = current.getCause();
    }
    return false;
  }
}
