package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Course;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CourseGateway {
  Mono<Course> save(Course course);

  Mono<Course> findById(Long id);

  Flux<Course> findByInstitutionId(String institutionId);

  Flux<Course> findAll();

  Mono<Void> deleteById(Long id);
}

