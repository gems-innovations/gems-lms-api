package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.EmailPreferencesUseCase;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Service-to-service only (not routed by the gateway): ms-education asks for a course notice to be
 * e-mailed. Needs both the shared internal key and the token of the user whose action caused it.
 */
@RestController
public class InternalNoticeController {
  private static final Logger log = LoggerFactory.getLogger(InternalNoticeController.class);

  private final EmailPreferencesUseCase preferences;
  private final byte[] internalKey;

  public InternalNoticeController(EmailPreferencesUseCase preferences, @Value("${jwt.secret}") String internalKey) {
    this.preferences = preferences;
    this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
  }

  /** To the given users, or (when {@code staffOfInstitution} is set) to that institution's teachers and admins. */
  public record NoticeRequest(List<Long> userIds, String subject, String message, String linkPath, String linkLabel,
                              String staffOfInstitution) {
  }

  public record TipRequest(Long userId, String subject, String message, String linkPath, String linkLabel) {
  }

  /** To an address that is not a user yet, or to every super admin when {@code to} is "super-admins". */
  public record AddressNoticeRequest(String to, String name, String subject, String message, String linkPath,
                                     String linkLabel) {
  }

  /** Answers at once; the e-mails are sent in the background so the caller is never slowed down. */
  @PostMapping("/internal/notifications/email")
  public Mono<ResponseEntity<Void>> send(@RequestHeader("X-Internal-Key") String key, @RequestBody NoticeRequest request) {
    if (!MessageDigest.isEqual(internalKey, key.getBytes(StandardCharsets.UTF_8))) {
      return Mono.error(new ForbiddenException("Invalid internal key"));
    }
    boolean toStaff = request != null && !isBlank(request.staffOfInstitution());
    if (request == null || (!toStaff && (request.userIds() == null || request.userIds().isEmpty()))
        || isBlank(request.subject()) || isBlank(request.message())) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    return CurrentUser.get().map(caller -> {
      String subject = trim(request.subject(), 120);
      String message = trim(request.message(), 1000);
      String label = trim(request.linkLabel(), 40);
      (toStaff
        ? preferences.sendToStaff(request.staffOfInstitution(), subject, message, request.linkPath(), label)
        : preferences.sendCourseNotice(request.userIds(), subject, message, request.linkPath(), label))
        .subscribe(sent -> log.debug("Course notice e-mailed to {} users", sent),
          error -> log.warn("Course notice e-mail failed: {}", error.getMessage()));
      return ResponseEntity.accepted().<Void>build();
    });
  }

  /**
   * For actions without a signed-in user (a public institution request): only the internal key is checked,
   * and this path is never routed by the gateway.
   */
  @PostMapping("/internal/notifications/email-address")
  public Mono<ResponseEntity<Void>> sendToAddress(@RequestHeader("X-Internal-Key") String key,
                                                  @RequestBody AddressNoticeRequest request) {
    if (!MessageDigest.isEqual(internalKey, key.getBytes(StandardCharsets.UTF_8))) {
      return Mono.error(new ForbiddenException("Invalid internal key"));
    }
    if (request == null || isBlank(request.to()) || isBlank(request.subject()) || isBlank(request.message())) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    String subject = trim(request.subject(), 120);
    String message = trim(request.message(), 2000);
    String label = trim(request.linkLabel(), 40);
    ("super-admins".equals(request.to())
      ? preferences.sendToSuperAdmins(subject, message, request.linkPath(), label).then()
      : preferences.sendToAddress(request.to(), trim(request.name(), 80), subject, message, request.linkPath(), label))
      .subscribe(done -> { }, error -> log.warn("Address notice e-mail failed: {}", error.getMessage()));
    return Mono.just(ResponseEntity.accepted().build());
  }

  /** Motivation e-mails from ms-education's daily job (no user session): internal key only. */
  @PostMapping("/internal/notifications/tip")
  public Mono<ResponseEntity<Void>> sendTip(@RequestHeader("X-Internal-Key") String key, @RequestBody TipRequest request) {
    if (!MessageDigest.isEqual(internalKey, key.getBytes(StandardCharsets.UTF_8))) {
      return Mono.error(new ForbiddenException("Invalid internal key"));
    }
    if (request == null || request.userId() == null || isBlank(request.subject()) || isBlank(request.message())) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    return preferences.sendTip(request.userId(), trim(request.subject(), 120), trim(request.message(), 1000),
        request.linkPath(), trim(request.linkLabel(), 40))
      .thenReturn(ResponseEntity.accepted().build());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String trim(String s, int max) {
    return s == null || s.length() <= max ? s : s.substring(0, max);
  }
}
