package com.gems.auth.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Modifying;
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

  @Modifying
  @Query("UPDATE users SET password = :encodedPassword, must_change_password = false, updated_at = NOW() WHERE user_id = :userId")
  Mono<Integer> updatePassword(Long userId, String encodedPassword);

  @Query("SELECT * FROM users WHERE institution_id = :institutionId AND (:pattern = '' OR first_name ILIKE :pattern OR last_name ILIKE :pattern OR email ILIKE :pattern OR username ILIKE :pattern) ORDER BY first_name, last_name, user_id LIMIT :limit OFFSET :offset")
  Flux<UserEntity> searchByInstitution(String institutionId, String pattern, int limit, long offset);

  @Query("SELECT COUNT(*) FROM users WHERE institution_id = :institutionId AND (:pattern = '' OR first_name ILIKE :pattern OR last_name ILIKE :pattern OR email ILIKE :pattern OR username ILIKE :pattern)")
  Mono<Long> countSearchByInstitution(String institutionId, String pattern);
}
