package com.gems.auth.application;

import com.gems.auth.application.gateway.ConsentGateway;
import com.gems.auth.application.gateway.CourseNoticeNotifier;
import com.gems.auth.application.gateway.EmailPreferencesGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Objects;

/** What e-mail each user receives, the one-click opt-out, and course notices sent on behalf of ms-education. */
public class EmailPreferencesUseCase {
  /** A single notice never reaches more people than this (a large course announcement). */
  public static final int MAX_RECIPIENTS = 1000;

  private final UserGateway users;
  private final EmailPreferencesGateway preferences;
  private final CourseNoticeNotifier notifier;
  private final PreferenceTokens tokens;
  private final ConsentGateway consents;
  private final String policyVersion;

  public EmailPreferencesUseCase(UserGateway users, EmailPreferencesGateway preferences, CourseNoticeNotifier notifier,
                                 PreferenceTokens tokens) {
    this(users, preferences, notifier, tokens, (userId, kind, version, granted) -> Mono.empty(), "");
  }

  public EmailPreferencesUseCase(UserGateway users, EmailPreferencesGateway preferences, CourseNoticeNotifier notifier,
                                 PreferenceTokens tokens, ConsentGateway consents, String policyVersion) {
    this.consents = consents;
    this.policyVersion = policyVersion;
    this.users = users;
    this.preferences = preferences;
    this.notifier = notifier;
    this.tokens = tokens;
  }

  public Mono<EmailPreferences> get(Long userId) {
    return preferences.find(userId);
  }

  /** Turning tips on or off leaves proof of the new choice (it is a promotional authorization). */
  public Mono<EmailPreferences> update(Long userId, EmailPreferences changed) {
    return preferences.find(userId)
      .flatMap(before -> before.tips() == changed.tips() ? Mono.<Void>empty()
        : consents.record(userId, ConsentGateway.TIPS, policyVersion, changed.tips()))
      .then(preferences.save(userId, changed))
      .thenReturn(changed);
  }

  /** From the link in an e-mail, without signing in. Empty if the token is not valid. */
  public Mono<EmailPreferences> getWithToken(String token) {
    return Mono.justOrEmpty(tokens.userOf(token)).flatMap(preferences::find);
  }

  public Mono<EmailPreferences> updateWithToken(String token, EmailPreferences changed) {
    return Mono.justOrEmpty(tokens.userOf(token)).flatMap(userId -> update(userId, changed));
  }

  /**
   * Sends the notice to each active, registered user who keeps course notices on. Guests have no real
   * address and are skipped. Emits how many e-mails were handed to the mail server.
   */
  public Mono<Long> sendCourseNotice(Collection<Long> userIds, String subject, String message, String linkPath,
                                     String linkLabel) {
    String path = linkPath != null && linkPath.startsWith("/") && !linkPath.startsWith("//") ? linkPath : "/learn/home";
    return Flux.fromIterable(userIds).filter(Objects::nonNull).distinct().take(MAX_RECIPIENTS)
      .concatMap(id -> users.findById(new UserId(id))
        .filter(user -> Boolean.TRUE.equals(user.isActive()) && !GuestAccessUseCase.isGuest(user.getEmail().getValue()))
        .filterWhen(user -> preferences.find(id).map(EmailPreferences::courseNotices))
        .flatMap(user -> notifier.sendNotice(user.getEmail().getValue(), user.getFirstName().getValue(), subject,
            message, path, linkLabel, tokens.create(id))
          .thenReturn(1L)
          .onErrorResume(e -> Mono.just(0L))))
      .reduce(0L, Long::sum);
  }

  /** Motivation e-mail: only to active, registered users who keep «tips» on; includes the opt-out link. */
  public Mono<Long> sendTip(Long userId, String subject, String message, String linkPath, String linkLabel) {
    return users.findById(new UserId(userId))
      .filter(user -> Boolean.TRUE.equals(user.isActive()) && !GuestAccessUseCase.isGuest(user.getEmail().getValue()))
      .filterWhen(user -> preferences.find(userId).map(EmailPreferences::tips))
      .flatMap(user -> notifier.sendNotice(user.getEmail().getValue(), user.getFirstName().getValue(), subject,
          message, safePath(linkPath), linkLabel, tokens.create(userId))
        .thenReturn(1L))
      .defaultIfEmpty(0L)
      .onErrorResume(e -> Mono.just(0L));
  }

  /** Course notice for the teachers and administrators of an institution (a delivery, a forum question). */
  public Mono<Long> sendToStaff(String institutionId, String subject, String message, String linkPath,
                                String linkLabel) {
    if (institutionId == null || institutionId.isBlank()) return Mono.just(0L);
    return users.findByInstitutionId(institutionId)
      .filter(user -> user.getRole() == UserRole.INSTRUCTOR || user.getRole() == UserRole.ADMIN)
      .map(user -> user.getId().getValue())
      .collectList()
      .flatMap(ids -> sendCourseNotice(ids, subject, message, linkPath, linkLabel));
  }

  /** Notice for every super admin (new institution requests). Not subject to course-notice preferences. */
  public Mono<Long> sendToSuperAdmins(String subject, String message, String linkPath, String linkLabel) {
    return users.findAll()
      .filter(user -> user.getRole() == UserRole.SUPER_ADMIN && Boolean.TRUE.equals(user.isActive()))
      .concatMap(user -> notifier.sendNotice(user.getEmail().getValue(), user.getFirstName().getValue(), subject,
          message, safePath(linkPath), linkLabel, null)
        .thenReturn(1L).onErrorResume(e -> Mono.just(0L)))
      .reduce(0L, Long::sum);
  }

  /**
   * About the account itself (created, password changed, activated). Always sent to registered users,
   * whatever their preferences, because it can matter for their security.
   */
  public Mono<Void> sendAccountNotice(Long userId, String subject, String message, String linkPath, String linkLabel) {
    return users.findById(new UserId(userId))
      .filter(user -> !GuestAccessUseCase.isGuest(user.getEmail().getValue()))
      .flatMap(user -> notifier.sendNotice(user.getEmail().getValue(), user.getFirstName().getValue(), subject,
        message, safePath(linkPath), linkLabel, null))
      .onErrorResume(e -> Mono.empty());
  }

  /** To someone who is not a user yet (whoever asked for an institution space). */
  public Mono<Void> sendToAddress(String email, String name, String subject, String message, String linkPath,
                                  String linkLabel) {
    if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return Mono.empty();
    return notifier.sendNotice(email.trim(), name, subject, message, safePath(linkPath), linkLabel, null)
      .onErrorResume(e -> Mono.empty());
  }

  private static String safePath(String linkPath) {
    return linkPath != null && linkPath.startsWith("/") && !linkPath.startsWith("//") ? linkPath : "/";
  }
}
