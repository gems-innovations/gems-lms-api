package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface IStudentRepository extends ReactiveCrudRepository<StudentEntity, Long> {
  Mono<StudentEntity> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);
  Mono<Boolean> existsByDocumentNumber(String documentNumber);
}