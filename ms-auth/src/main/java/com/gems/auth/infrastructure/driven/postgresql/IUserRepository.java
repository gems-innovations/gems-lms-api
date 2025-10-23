package com.gems.auth.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface IUserRepository extends ReactiveCrudRepository<UserEntity, Long> {
  Mono<UserEntity> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);
}
