package com.gems.auth.application;

import com.gems.auth.application.gateway.EmailVerificationGateway;
import com.gems.auth.application.gateway.EmailVerificationNotifier;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/** Confirms that a user owns their e-mail address: a link is sent and opening it marks the address verified. */
public class EmailVerificationUseCase {
  private static final Duration TOKEN_TTL = Duration.ofHours(24);
  private static final Duration RESEND_GAP = Duration.ofSeconds(60);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserGateway users;
  private final EmailVerificationGateway gateway;
  private final EmailVerificationNotifier notifier;

  public EmailVerificationUseCase(UserGateway users, EmailVerificationGateway gateway, EmailVerificationNotifier notifier) {
    this.users = users;
    this.gateway = gateway;
    this.notifier = notifier;
  }

  /** Sends the link unless the account is a guest, inactive, already verified or was sent one a minute ago. */
  public Mono<Void> send(Long userId) {
    return users.findById(new UserId(userId))
      .filter(user -> Boolean.TRUE.equals(user.isActive()) && !GuestAccessUseCase.isGuest(user.getEmail().getValue()))
      .flatMap(user -> gateway.isVerified(userId).flatMap(verified -> {
        if (Boolean.TRUE.equals(verified)) {
          return Mono.<Void>empty();
        }
        LocalDateTime now = LocalDateTime.now();
        return gateway.lastSentAt(userId)
          .map(sentAt -> sentAt.isAfter(now.minus(RESEND_GAP)))
          .defaultIfEmpty(false)
          .flatMap(tooSoon -> {
            if (tooSoon) {
              return Mono.<Void>empty();
            }
            String token = newToken();
            return gateway.issue(userId, PasswordRecoveryUseCase.hash(token), now.plus(TOKEN_TTL), now)
              .then(Mono.defer(() -> notifier.sendVerification(
                user.getEmail().getValue(), user.getFirstName().getValue(), token)));
          });
      }));
  }

  /** True if the token was valid and the e-mail is now verified. */
  public Mono<Boolean> confirm(String token) {
    if (token == null || token.isBlank()) {
      return Mono.just(false);
    }
    return gateway.confirm(PasswordRecoveryUseCase.hash(token), LocalDateTime.now())
      .map(userId -> true)
      .defaultIfEmpty(false);
  }

  public Mono<Boolean> isVerified(Long userId) {
    return gateway.isVerified(userId);
  }

  private static String newToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
