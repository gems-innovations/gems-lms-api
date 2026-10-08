package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Course;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CourseGateway {
  Mono<Course> save(Course course);

  Mono<Course> findById(Long id);

  /** The course without its modules, lessons and contents: one query instead of four. */
  Mono<Course> findHeaderById(Long id);

  Mono<Long> findCourseIdByLessonId(Long lessonId);

  /** The institution's courses without their modules, lessons and contents (for counts, reports, lookups). */
  Flux<Course> findHeadersByInstitutionId(String institutionId);

  Flux<Course> findByInstitutionId(String institutionId);

  Flux<Course> findAll();

  Flux<Course> findPage(String search, String status, String difficulty, String institutionId, int offset, int limit);

  Mono<Long> count(String search, String status, String difficulty, String institutionId);

  Mono<Void> deleteById(Long id);
}
