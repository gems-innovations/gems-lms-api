package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.Enrollment;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public class EnrollmentRepositoryAdapter implements EnrollmentGateway {
  private final IEnrollmentRepository enrollmentRepository;

  public EnrollmentRepositoryAdapter(IEnrollmentRepository enrollmentRepository) {
    this.enrollmentRepository = enrollmentRepository;
  }

  @Override
  public Mono<Enrollment> save(Enrollment enrollment) {
    LocalDateTime enrolledAt = enrollment.getEnrolledAt() != null ? enrollment.getEnrolledAt() : LocalDateTime.now();
    EnrollmentEntity entity = new EnrollmentEntity(
      enrollment.getId(),
      enrollment.getStudentId(),
      enrollment.getCourseId(),
      enrolledAt,
      enrollment.getProgress() != null ? enrollment.getProgress() : 0,
      enrollment.getCompletedAt()
    );
    return enrollmentRepository.save(entity)
      .map(this::mapToDomain);
  }

  @Override
  public Mono<Enrollment> findById(Long id) {
    return enrollmentRepository.findById(id)
      .map(this::mapToDomain);
  }

  @Override
  public Flux<Enrollment> findByStudentId(Long studentId) {
    return enrollmentRepository.findByStudentId(studentId)
      .map(this::mapToDomain);
  }

  @Override
  public Mono<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId) {
    return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
      .map(this::mapToDomain);
  }

  @Override
  public Mono<Boolean> existsByStudentIdAndCourseId(Long studentId, Long courseId) {
    return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
  }

  @Override
  public Mono<Void> deleteById(Long id) {
    return enrollmentRepository.deleteById(id);
  }

  private Enrollment mapToDomain(EnrollmentEntity entity) {
    return new Enrollment(
      entity.getId(),
      entity.getStudentId(),
      entity.getCourseId(),
      entity.getEnrolledAt(),
      entity.getProgress(),
      entity.getCompletedAt()
    );
  }
}
