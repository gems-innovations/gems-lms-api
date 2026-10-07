package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.application.gateway.PasswordResetGateway;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public class PasswordResetRepositoryAdapter implements PasswordResetGateway {
  private final IPasswordResetTokenRepository repository;

  public PasswordResetRepositoryAdapter(IPasswordResetTokenRepository repository) {
    this.repository = repository;
  }

  @Override
  public Mono<Void> save(Long userId, String tokenHash, LocalDateTime expiresAt) {
    PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
    entity.setUserId(userId);
    entity.setTokenHash(tokenHash);
    entity.setExpiresAt(expiresAt);
    entity.setCreatedAt(LocalDateTime.now());
    return repository.save(entity).then();
  }

  @Override
  public Mono<Long> findValidUser(String tokenHash, LocalDateTime now) {
    return repository.findValid(tokenHash, now).map(PasswordResetTokenEntity::getUserId);
  }

  @Override
  public Mono<Boolean> markUsed(String tokenHash, LocalDateTime now) {
    return repository.markUsed(tokenHash, now).map(rows -> rows > 0);
  }
}
