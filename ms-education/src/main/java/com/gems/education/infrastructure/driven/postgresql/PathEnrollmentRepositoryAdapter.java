package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.PathEnrollmentGateway;
import com.gems.education.domain.entities.PathEnrollment;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PathEnrollmentRepositoryAdapter implements PathEnrollmentGateway {
  private final IPathEnrollmentRepository repository;

  public PathEnrollmentRepositoryAdapter(IPathEnrollmentRepository repository) {
    this.repository = repository;
  }

  @Override
  public Mono<PathEnrollment> save(PathEnrollment enrollment) {
    PathEnrollmentEntity e = new PathEnrollmentEntity();
    e.setId(enrollment.id());
    e.setLearningPathId(enrollment.learningPathId());
    e.setStudentId(enrollment.studentId());
    e.setStatus(enrollment.status());
    e.setEnrolledAt(enrollment.enrolledAt());
    e.setCompletedAt(enrollment.completedAt());
    return repository.save(e).map(this::toDomain);
  }

  @Override
  public Mono<PathEnrollment> find(Long learningPathId, Long studentId) {
    return repository.findByLearningPathIdAndStudentId(learningPathId, studentId).map(this::toDomain);
  }

  @Override
  public Flux<PathEnrollment> findByStudent(Long studentId) {
    return repository.findByStudentIdOrderByEnrolledAtAsc(studentId).map(this::toDomain);
  }

  @Override
  public Flux<PathEnrollment> findByPath(Long learningPathId) {
    return repository.findByLearningPathIdOrderByEnrolledAtAsc(learningPathId).map(this::toDomain);
  }

  private PathEnrollment toDomain(PathEnrollmentEntity e) {
    return new PathEnrollment(e.getId(), e.getLearningPathId(), e.getStudentId(), e.getStatus(),
      e.getEnrolledAt(), e.getCompletedAt());
  }
}
