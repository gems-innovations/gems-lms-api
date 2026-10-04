package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.Enrollment;
import org.springframework.stereotype.Repository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public class EnrollmentRepositoryAdapter implements EnrollmentGateway {
  private final IEnrollmentRepository enrollmentRepository;
  private final DatabaseClient db;
  private final TransactionalOperator tx;

  public EnrollmentRepositoryAdapter(IEnrollmentRepository enrollmentRepository, DatabaseClient db,
                                     TransactionalOperator tx) {
    this.enrollmentRepository = enrollmentRepository;
    this.db = db;
    this.tx = tx;
  }

  @Override
  public Mono<Enrollment> saveRespectingCapacity(Enrollment enrollment) {
    LocalDateTime enrolledAt = enrollment.getEnrolledAt() != null ? enrollment.getEnrolledAt() : LocalDateTime.now();
    EnrollmentEntity entity = new EnrollmentEntity(null, enrollment.getStudentId(), enrollment.getCourseId(),
      enrollment.getStatus() != null ? enrollment.getStatus() : "active", enrolledAt,
      enrollment.getProgress() != null ? enrollment.getProgress() : 0, enrollment.getCompletedAt());
    entity.setProgressData(enrollment.getProgressData());

    Mono<Enrollment> operation = db.sql("""
        UPDATE courses c SET enrolled_count = COALESCE(c.enrolled_count, 0) + 1
        WHERE c.id = :courseId AND (
          (SELECT r.capacity FROM course_enrollment_rules r WHERE r.course_id = c.id) IS NULL
          OR COALESCE(c.enrolled_count, 0) <
             (SELECT r.capacity FROM course_enrollment_rules r WHERE r.course_id = c.id)
        )
        """)
      .bind("courseId", enrollment.getCourseId())
      .fetch().rowsUpdated()
      .filter(updated -> updated == 1)
      .flatMap(ignored -> enrollmentRepository.save(entity))
      .map(this::mapToDomain);
    return tx.transactional(operation);
  }

  @Override
  public Mono<Enrollment> save(Enrollment enrollment) {
    LocalDateTime enrolledAt = enrollment.getEnrolledAt() != null ? enrollment.getEnrolledAt() : LocalDateTime.now();
    EnrollmentEntity entity = new EnrollmentEntity(
      enrollment.getId(),
      enrollment.getStudentId(),
      enrollment.getCourseId(),
      enrollment.getStatus() != null ? enrollment.getStatus() : "active",
      enrolledAt,
      enrollment.getProgress() != null ? enrollment.getProgress() : 0,
      enrollment.getCompletedAt()
    );
    entity.setProgressData(enrollment.getProgressData());
    return enrollmentRepository.save(entity)
      .flatMap(saved -> enrollmentRepository.refreshCourseStats(saved.getCourseId()).thenReturn(saved))
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
  public Flux<Enrollment> findByInstitutionId(String institutionId) {
    return enrollmentRepository.findByInstitutionId(institutionId)
      .map(this::mapToDomain);
  }

  @Override
  public Flux<Enrollment> findByCourseId(Long courseId) {
    return enrollmentRepository.findByCourseId(courseId)
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
    return enrollmentRepository.findById(id)
      .flatMap(entity -> enrollmentRepository.deleteById(id)
        .then(enrollmentRepository.refreshCourseStats(entity.getCourseId())))
      .then();
  }

  private Enrollment mapToDomain(EnrollmentEntity entity) {
    Enrollment enrollment = new Enrollment(
      entity.getId(),
      entity.getStudentId(),
      entity.getCourseId(),
      entity.getStatus(),
      entity.getEnrolledAt(),
      entity.getProgress(),
      entity.getCompletedAt()
    );
    enrollment.setProgressData(entity.getProgressData());
    return enrollment;
  }
}
