package com.gems.auth.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface IPasswordResetTokenRepository extends ReactiveCrudRepository<PasswordResetTokenEntity, Long> {
  @Query("SELECT * FROM password_reset_tokens WHERE token_hash = :tokenHash AND used_at IS NULL AND expires_at > :now")
  Mono<PasswordResetTokenEntity> findValid(String tokenHash, LocalDateTime now);

  /** Returns 1 if this call used the token, 0 if it was already used. */
  @Modifying
  @Query("UPDATE password_reset_tokens SET used_at = :now WHERE token_hash = :tokenHash AND used_at IS NULL")
  Mono<Integer> markUsed(String tokenHash, LocalDateTime now);
}
