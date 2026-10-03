package com.gems.education.infrastructure.driven.files;

import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/**
 * Where uploaded bytes live. {@link LocalFileStorage} keeps them on disk; an object store
 * (S3, GCS, Azure Blob) can replace it by implementing this interface.
 */
public interface FileStorage {
  /** Stores the part under {@code key} and emits its size in bytes. */
  Mono<Long> write(String key, FilePart part);

  /** The stored content, or empty if there is none. */
  Mono<Resource> read(String key);

  Mono<Void> delete(String key);
}
