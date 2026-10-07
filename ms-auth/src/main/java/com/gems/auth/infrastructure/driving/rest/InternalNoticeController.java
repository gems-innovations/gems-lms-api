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

  public record NoticeRequest(List<Long> userIds, String subject, String message, String linkPath, String linkLabel) {
  }

  /** Answers at once; the e-mails are sent in the background so the caller is never slowed down. */
  @PostMapping("/internal/notifications/email")
  public Mono<ResponseEntity<Void>> send(@RequestHeader("X-Internal-Key") String key, @RequestBody NoticeRequest request) {
    if (!MessageDigest.isEqual(internalKey, key.getBytes(StandardCharsets.UTF_8))) {
      return Mono.error(new ForbiddenException("Invalid internal key"));
    }
    if (request == null || request.userIds() == null || request.userIds().isEmpty()
        || isBlank(request.subject()) || isBlank(request.message())) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    return CurrentUser.get().map(caller -> {
      preferences.sendCourseNotice(request.userIds(), trim(request.subject(), 120), trim(request.message(), 1000),
          request.linkPath(), trim(request.linkLabel(), 40))
        .subscribe(sent -> log.debug("Course notice e-mailed to {} users", sent),
          error -> log.warn("Course notice e-mail failed: {}", error.getMessage()));
      return ResponseEntity.accepted().<Void>build();
    });
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String trim(String s, int max) {
    return s == null || s.length() <= max ? s : s.substring(0, max);
  }
}
