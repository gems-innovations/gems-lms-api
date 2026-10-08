package com.gems.education.application;

import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.Enrollment;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static com.gems.education.TestData.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InstitutionReportUseCaseTest {
  private final CourseGateway courses = mock(CourseGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final CourseActivityGateway activity = mock(CourseActivityGateway.class);
  private final InstitutionReportUseCase useCase = new InstitutionReportUseCase(courses, enrollments, activity);

  @Test
  void aggregatesCoursesEnrollmentsProgressAndPendingDeliveries() {
    LocalDateTime now = LocalDateTime.now();
    when(courses.findHeadersByInstitutionId("inst-1")).thenReturn(Flux.just(
      course(1L, "Docker", "", "published", "inst-1", now, now, List.of()),
      course(2L, "Java", "", "draft", "inst-1", now, now, List.of())));
    Enrollment completed = new Enrollment(1L, 10L, 1L, "completed", now, 100, now);
    Enrollment active = new Enrollment(2L, 11L, 1L, "active", now, 50, null);
    Enrollment cancelled = new Enrollment(3L, 10L, 2L, "cancelled", now, 0, null);
    when(enrollments.findByInstitutionId("inst-1")).thenReturn(Flux.just(completed, active, cancelled));
    when(activity.findSubmissionsByInstitution("inst-1")).thenReturn(Flux.just(
      submission(1L, AssignmentSubmission.PENDING), submission(2L, AssignmentSubmission.GRADED)));

    StepVerifier.create(useCase.execute("inst-1")).assertNext(report -> {
      assertThat(report.totalCourses()).isEqualTo(2);
      assertThat(report.totalEnrollments()).isEqualTo(3);
      assertThat(report.activeStudents()).isEqualTo(2);
      assertThat(report.activeEnrollments()).isEqualTo(1);
      assertThat(report.completedEnrollments()).isEqualTo(1);
      assertThat(report.completionRate()).isEqualTo(33.3);
      assertThat(report.averageProgress()).isEqualTo(50.0);
      assertThat(report.pendingSubmissions()).isEqualTo(1);
      assertThat(report.courses().get(0).completionRate()).isEqualTo(50.0);
      assertThat(report.courses().get(0).pendingSubmissions()).isEqualTo(1);
    }).verifyComplete();
  }

  private static AssignmentSubmission submission(Long id, String status) {
    return new AssignmentSubmission(id, 1L, 10L, 1L, 2L, 3L, "", "[]", LocalDateTime.now(),
      null, null, status, null);
  }
}
