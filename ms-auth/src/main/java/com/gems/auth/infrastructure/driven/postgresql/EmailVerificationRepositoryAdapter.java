package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.application.gateway.EmailVerificationGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public class EmailVerificationRepositoryAdapter implements EmailVerificationGateway {
  private final DatabaseClient db;

  public EmailVerificationRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Void> issue(Long userId, String tokenHash, LocalDateTime expiresAt, LocalDateTime now) {
    return db.sql("INSERT INTO email_verifications (user_id, token_hash, expires_at, sent_at) "
        + "VALUES (:userId, :hash, :expires, :now) "
        + "ON CONFLICT (user_id) DO UPDATE SET token_hash = EXCLUDED.token_hash, "
        + "expires_at = EXCLUDED.expires_at, sent_at = EXCLUDED.sent_at "
        + "WHERE email_verifications.verified_at IS NULL")
      .bind("userId", userId).bind("hash", tokenHash).bind("expires", expiresAt).bind("now", now)
      .then();
  }

  @Override
  public Mono<Long> confirm(String tokenHash, LocalDateTime now) {
    return db.sql("UPDATE email_verifications SET verified_at = :now "
        + "WHERE token_hash = :hash AND verified_at IS NULL AND expires_at > :now RETURNING user_id")
      .bind("hash", tokenHash).bind("now", now)
      .map(row -> row.get("user_id", Long.class)).one();
  }

  @Override
  public Mono<Boolean> isVerified(Long userId) {
    return db.sql("SELECT 1 AS ok FROM email_verifications WHERE user_id = :userId AND verified_at IS NOT NULL")
      .bind("userId", userId).map(row -> true).one().defaultIfEmpty(false);
  }

  @Override
  public Mono<LocalDateTime> lastSentAt(Long userId) {
    return db.sql("SELECT sent_at FROM email_verifications WHERE user_id = :userId")
      .bind("userId", userId).map(row -> row.get("sent_at", LocalDateTime.class)).one();
  }
}
