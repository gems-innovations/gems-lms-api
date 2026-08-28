package com.gems.education.application.gateway;

import com.gems.education.domain.entities.LearningPath;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LearningPathGateway {
  Mono<LearningPath> save(LearningPath learningPath);
  Mono<LearningPath> findById(Long id);
  Flux<LearningPath> findByInstitutionId(String institutionId);
  Flux<LearningPath> findAll();
  Mono<Void> deleteById(Long id);
}

