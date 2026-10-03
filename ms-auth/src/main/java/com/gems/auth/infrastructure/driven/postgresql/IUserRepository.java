package com.gems.auth.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface IUserRepository extends ReactiveCrudRepository<UserEntity, Long> {
  Mono<UserEntity> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);

  Mono<Boolean> existsByUsername(String username);

  Flux<UserEntity> findByInstitutionId(String institutionId);

  @Query("SELECT institution_id, COUNT(*) AS total FROM users WHERE active AND institution_id IS NOT NULL GROUP BY institution_id")
  Flux<InstitutionUserCount> countActiveByInstitution();
}
