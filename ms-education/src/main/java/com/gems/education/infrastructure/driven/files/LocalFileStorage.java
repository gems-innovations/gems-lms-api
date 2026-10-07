package com.gems.education.infrastructure.driven.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps uploads in a directory of the server ({@code files.dir}, ./data/uploads by default). */
@Component
@ConditionalOnProperty(name = "files.storage", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {
  private final Path root;

  public LocalFileStorage(@Value("${files.dir:./data/uploads}") String dir) {
    this.root = Path.of(dir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create the uploads directory " + root, e);
    }
  }

  @Override
  public Mono<Long> write(String key, FilePart part) {
    Path target = resolve(key);
    return part.transferTo(target)
      .then(Mono.fromCallable(() -> Files.size(target)).subscribeOn(Schedulers.boundedElastic()));
  }

  @Override
  public Mono<Resource> read(String key) {
    return Mono.fromCallable(() -> resolve(key))
      .filter(Files::isRegularFile)
      .map(path -> (Resource) new FileSystemResource(path))
      .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<Void> delete(String key) {
    return Mono.fromCallable(() -> Files.deleteIfExists(resolve(key)))
      .subscribeOn(Schedulers.boundedElastic())
      .then();
  }

  /** Keys are server-generated UUIDs; anything else is rejected so a key never leaves {@link #root}. */
  private Path resolve(String key) {
    if (!key.matches("[0-9a-f\\-]{36}")) throw new IllegalArgumentException("Invalid file key");
    return root.resolve(key);
  }
}
