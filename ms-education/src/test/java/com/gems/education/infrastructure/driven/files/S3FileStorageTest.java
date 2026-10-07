package com.gems.education.infrastructure.driven.files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3FileStorageTest {
  private static final String ID = "123e4567-e89b-12d3-a456-426614174000";
  private S3AsyncClient client;
  private S3FileStorage storage;

  @BeforeEach
  void setUp() {
    client = mock(S3AsyncClient.class);
    storage = new S3FileStorage(client, "private-bucket", "/institutions/one/");
  }

  @Test
  void uploadsUsingTheConfiguredPrefixAndReturnsTheSize() {
    byte[] content = "course material".getBytes();
    FilePart part = mock(FilePart.class);
    when(part.transferTo(any(Path.class))).thenAnswer(invocation -> {
      Path target = invocation.getArgument(0);
      return Mono.fromRunnable(() -> {
        try { Files.write(target, content); }
        catch (Exception exception) { throw new RuntimeException(exception); }
      });
    });
    when(client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
      .thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));

    StepVerifier.create(storage.write(ID, part)).expectNext((long) content.length).verifyComplete();

    var request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(client).putObject(request.capture(), any(AsyncRequestBody.class));
    assertEquals("private-bucket", request.getValue().bucket());
    assertEquals("institutions/one/" + ID, request.getValue().key());
  }

  @Test
  void readsAnObjectAsAResource() {
    byte[] content = { 1, 2, 3 };
    var bytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content);
    when(client.getObject(any(GetObjectRequest.class), responseTransformer()))
      .thenReturn(CompletableFuture.completedFuture(bytes));

    StepVerifier.create(storage.read(ID))
      .assertNext(resource -> {
        try { assertArrayEquals(content, resource.getContentAsByteArray()); }
        catch (Exception exception) { throw new RuntimeException(exception); }
      })
      .verifyComplete();
  }

  @Test
  void missingObjectsAreAnEmptyResult() {
    var failure = CompletableFuture.<ResponseBytes<GetObjectResponse>>failedFuture(
      NoSuchKeyException.builder().message("missing").build());
    when(client.getObject(any(GetObjectRequest.class), responseTransformer())).thenReturn(failure);

    StepVerifier.create(storage.read(ID)).verifyComplete();
  }

  @Test
  void deletesThePrefixedObject() {
    when(client.deleteObject(any(DeleteObjectRequest.class)))
      .thenReturn(CompletableFuture.completedFuture(DeleteObjectResponse.builder().build()));

    StepVerifier.create(storage.delete(ID)).verifyComplete();

    var request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(client).deleteObject(request.capture());
    assertEquals("institutions/one/" + ID, request.getValue().key());
  }

  @Test
  void rejectsInvalidKeysBeforeCallingS3() {
    assertThrows(IllegalArgumentException.class, () -> storage.delete("../secret"));
  }

  @SuppressWarnings("unchecked")
  private AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> responseTransformer() {
    return any(AsyncResponseTransformer.class);
  }
}
