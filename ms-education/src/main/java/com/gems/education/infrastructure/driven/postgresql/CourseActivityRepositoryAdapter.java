package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.QuizAttempt;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class CourseActivityRepositoryAdapter implements CourseActivityGateway {
  private final IQuizAttemptRepository attemptRepository;
  private final IAssignmentSubmissionRepository submissionRepository;

  public CourseActivityRepositoryAdapter(IQuizAttemptRepository attemptRepository,
                                         IAssignmentSubmissionRepository submissionRepository) {
    this.attemptRepository = attemptRepository;
    this.submissionRepository = submissionRepository;
  }

  @Override
  public Mono<QuizAttempt> saveAttempt(QuizAttempt attempt) {
    QuizAttemptEntity e = new QuizAttemptEntity();
    e.setId(attempt.id());
    e.setEnrollmentId(attempt.enrollmentId());
    e.setStudentId(attempt.studentId());
    e.setCourseId(attempt.courseId());
    e.setBlockId(attempt.blockId());
    e.setLessonId(attempt.lessonId());
    e.setAttemptNumber(attempt.attemptNumber());
    e.setAnswers(attempt.answers());
    e.setScore(attempt.score());
    e.setPassed(attempt.passed());
    e.setFeedback(attempt.feedback());
    e.setCompletedAt(attempt.completedAt());
    return attemptRepository.save(e).map(this::toAttempt);
  }

  @Override
  public Flux<QuizAttempt> findAttemptsByStudent(Long studentId) {
    return attemptRepository.findByStudentIdOrderByCompletedAtAsc(studentId).map(this::toAttempt);
  }

  @Override
  public Mono<Long> countAttempts(Long enrollmentId, Long blockId) {
    return attemptRepository.countByEnrollmentIdAndBlockId(enrollmentId, blockId);
  }

  @Override
  public Mono<AssignmentSubmission> saveSubmission(AssignmentSubmission s) {
    AssignmentSubmissionEntity e = new AssignmentSubmissionEntity();
    e.setId(s.id());
    e.setEnrollmentId(s.enrollmentId());
    e.setStudentId(s.studentId());
    e.setCourseId(s.courseId());
    e.setBlockId(s.blockId());
    e.setLessonId(s.lessonId());
    e.setTextContent(s.textContent());
    e.setFileUrls(s.fileUrls());
    e.setSubmittedAt(s.submittedAt());
    e.setGrade(s.grade());
    e.setFeedback(s.feedback());
    e.setStatus(s.status());
    return submissionRepository.save(e).map(this::toSubmission);
  }

  @Override
  public Mono<AssignmentSubmission> findSubmission(Long id) {
    return submissionRepository.findById(id).map(this::toSubmission);
  }

  @Override
  public Mono<AssignmentSubmission> findSubmission(Long enrollmentId, Long blockId) {
    return submissionRepository.findByEnrollmentIdAndBlockId(enrollmentId, blockId).map(this::toSubmission);
  }

  @Override
  public Flux<AssignmentSubmission> findSubmissionsByStudent(Long studentId) {
    return submissionRepository.findByStudentId(studentId).map(this::toSubmission);
  }

  @Override
  public Flux<AssignmentSubmission> findSubmissionsByCourse(Long courseId) {
    return submissionRepository.findByCourseIdOrderBySubmittedAtDesc(courseId).map(this::toSubmission);
  }

  @Override
  public Flux<AssignmentSubmission> findSubmissionsByInstitution(String institutionId) {
    return submissionRepository.findByInstitutionId(institutionId).map(this::toSubmission);
  }

  private QuizAttempt toAttempt(QuizAttemptEntity e) {
    return new QuizAttempt(e.getId(), e.getEnrollmentId(), e.getStudentId(), e.getCourseId(), e.getBlockId(),
      e.getLessonId(), e.getAttemptNumber(), e.getAnswers(), e.getScore(), Boolean.TRUE.equals(e.getPassed()),
      e.getFeedback(), e.getCompletedAt());
  }

  private AssignmentSubmission toSubmission(AssignmentSubmissionEntity e) {
    return new AssignmentSubmission(e.getId(), e.getEnrollmentId(), e.getStudentId(), e.getCourseId(),
      e.getBlockId(), e.getLessonId(), e.getTextContent(), e.getFileUrls(), e.getSubmittedAt(), e.getGrade(),
      e.getFeedback(), e.getStatus());
  }
}
