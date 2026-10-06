package com.gems.education.infrastructure.driving.rest;

import com.gems.education.infrastructure.driven.files.FileStorage;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import io.r2dbc.spi.Readable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uploads. Two scopes:
 * <ul>
 *   <li>{@code public}: images (course and path thumbnails, institution logos), served without a
 *   token at {@code /files/public/{id}} so {@code <img>} tags can load them;</li>
 *   <li>{@code private} (default): anything else, e.g. assignment deliveries; only the uploader
 *   and the staff of their institution can download them.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
  static final String PUBLIC = "public";
  static final String PRIVATE = "private";
  /** Foto de perfil: cualquier usuario la sube; se guarda como pública, solo imágenes rasterizadas. */
  static final String AVATAR = "avatar";
  static final long AVATAR_MAX_BYTES = 2L * 1024 * 1024;
  private static final java.util.Set<String> AVATAR_TYPES = java.util.Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

  private final FileStorage storage;
  private final DatabaseClient db;
  private final long maxBytes;

  public FileController(FileStorage storage, DatabaseClient db,
                        @Value("${files.max-size-bytes:10485760}") long maxBytes) {
    this.storage = storage;
    this.db = db;
    this.maxBytes = maxBytes;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<FileBody>> upload(@RequestPart("file") Mono<FilePart> filePart,
                                               @RequestParam(defaultValue = PRIVATE) String scope) {
    if (!PUBLIC.equals(scope) && !PRIVATE.equals(scope) && !AVATAR.equals(scope)) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be public, private or avatar"));
    }
    boolean avatar = AVATAR.equals(scope);
    String storedScope = avatar ? PUBLIC : scope;
    long limit = avatar ? Math.min(AVATAR_MAX_BYTES, maxBytes) : maxBytes;
    return CurrentUser.get().zipWith(filePart).flatMap(t -> {
      AuthenticatedUser caller = t.getT1();
      FilePart part = t.getT2();
      MediaType type = part.headers().getContentType() != null ? part.headers().getContentType()
        : MediaType.APPLICATION_OCTET_STREAM;
      // Public: images and WebVTT subtitles (the <track> of a video loads them without a token).
      boolean subtitles = "text".equals(type.getType()) && "vtt".equals(type.getSubtype());
      if (PUBLIC.equals(scope) && !"image".equals(type.getType()) && !subtitles) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only images and .vtt subtitles can be public"));
      }
      // SVG queda fuera: puede llevar scripts y el avatar se sirve sin token.
      if (avatar && !AVATAR_TYPES.contains(type.getType() + "/" + type.getSubtype())) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a PNG, JPG, WebP or GIF image"));
      }
      if (PUBLIC.equals(scope) && !caller.isStaff()) {
        return Mono.error(new ForbiddenException("Only staff upload public images"));
      }
      String id = UUID.randomUUID().toString();
      String name = safeName(part.filename());
      return storage.write(id, part).flatMap(size -> {
        if (size > limit) {
          return storage.delete(id).then(Mono.error(new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
            "Files are limited to " + (limit / (1024 * 1024)) + " MB")));
        }
        return insert(id, caller, storedScope, name, type.toString(), size)
          .thenReturn(new FileBody(id, name, type.toString(), size, url(id, storedScope)));
      });
    }).map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
  }

  /** Public images, no token needed. */
  @GetMapping("/public/{id}")
  public Mono<ResponseEntity<Resource>> publicFile(@PathVariable String id) {
    return find(id)
      .filter(f -> PUBLIC.equals(f.scope()))
      .flatMap(f -> serve(f, CacheControl.maxAge(Duration.ofDays(7)).cachePublic()))
      .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<Resource>> file(@PathVariable String id) {
    return CurrentUser.get().flatMap(caller -> find(id)
      .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")))
      .flatMap(f -> canRead(caller, f)
        ? serve(f, CacheControl.noStore())
        : Mono.error(new ForbiddenException("You cannot download this file"))));
  }

  private static boolean canRead(AuthenticatedUser caller, StoredFile f) {
    return PUBLIC.equals(f.scope()) || caller.isUser(f.ownerId())
      || (caller.isStaff() && caller.belongsTo(f.institutionId()));
  }

  private Mono<ResponseEntity<Resource>> serve(StoredFile f, CacheControl cache) {
    return storage.read(f.id()).map(resource -> ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(f.contentType()))
      .cacheControl(cache)
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
        .builder(PUBLIC.equals(f.scope()) ? "inline" : "attachment")
        .filename(f.name(), StandardCharsets.UTF_8).build().toString())
      .body(resource));
  }

  private Mono<Void> insert(String id, AuthenticatedUser caller, String scope, String name, String type, long size) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO stored_files "
        + "(id, owner_id, institution_id, scope, name, content_type, size_bytes, created_at) "
        + "VALUES (:id, :owner, :institution, :scope, :name, :type, :size, :createdAt)")
      .bind("id", id).bind("owner", caller.userId()).bind("scope", scope).bind("name", name)
      .bind("type", type).bind("size", size).bind("createdAt", LocalDateTime.now());
    spec = caller.institutionId() == null ? spec.bindNull("institution", String.class)
      : spec.bind("institution", caller.institutionId());
    return spec.then();
  }

  private Mono<StoredFile> find(String id) {
    if (!id.matches("[0-9a-f\\-]{36}")) return Mono.empty();
    return db.sql("SELECT * FROM stored_files WHERE id = :id").bind("id", id)
      .map(FileController::toStoredFile).one();
  }

  private static StoredFile toStoredFile(Readable row) {
    return new StoredFile(row.get("id", String.class), row.get("owner_id", Long.class),
      row.get("institution_id", String.class), row.get("scope", String.class), row.get("name", String.class),
      row.get("content_type", String.class));
  }

  static String url(String id, String scope) {
    return PUBLIC.equals(scope) ? "/api/v1/files/public/" + id : "/api/v1/files/" + id;
  }

  /** The original file name without any path, control characters or quotes. */
  static String safeName(String filename) {
    String base = filename == null ? "archivo" : filename.replace('\\', '/');
    base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").trim();
    if (base.isEmpty()) base = "archivo";
    return base.length() > 200 ? base.substring(base.length() - 200) : base;
  }

  record StoredFile(String id, Long ownerId, String institutionId, String scope, String name, String contentType) {
  }

  /** {@code url} is relative to the API origin (e.g. http://localhost:8080). */
  public record FileBody(String id, String name, String contentType, long size, String url) {
  }
}
