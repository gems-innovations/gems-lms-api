package com.gems.education.application;

import com.gems.education.TestData;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import com.gems.education.domain.entities.QuizAttempt;
import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.gateway.PathEnrollmentGateway;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.domain.entities.LearningPathStep;
import com.gems.education.domain.entities.PathEnrollment;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PathEnrollmentUseCaseTest {
  private final PathEnrollmentGateway gateway = mock(PathEnrollmentGateway.class);
  private final LearningPathGateway paths = mock(LearningPathGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final CourseActivityGateway activity = mock(CourseActivityGateway.class);
  private final PathEnrollmentUseCase useCase = new PathEnrollmentUseCase(gateway, paths, enrollments, activity);

  {
    lenient().when(activity.findAttemptsByStudent(any())).thenReturn(Flux.empty());
    lenient().when(activity.findSubmissionsByStudent(any())).thenReturn(Flux.empty());
  }

  @Test
  void progressCountsOnlyRequiredCoursesAndCompletesThePath() {
    LearningPath path = new LearningPath(
      7L, "Ruta", null, "inst-1", LocalDateTime.now(), List.of(
        TestData.course(1L, "A", null, "published", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of()), TestData.course(2L, "B", null, "published", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of()),
        TestData.course(3L, "C", null, "published", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of())));
    path.setSteps(List.of(new LearningPathStep(3L, false, null)));
    PathEnrollment active = new PathEnrollment(1L, 7L, 5L, PathEnrollment.ACTIVE, LocalDateTime.now(), null);
    Enrollment c1 =
      TestData.enrollment(10L, 5L, 1L, LocalDateTime.now(), 100, LocalDateTime.now());
    c1.setStatus("completed");
    Enrollment c2 =
      TestData.enrollment(11L, 5L, 2L, LocalDateTime.now(), 40, null);
    when(enrollments.findByStudentId(5L)).thenReturn(Flux.just(c1, c2));
    when(gateway.findByStudent(5L)).thenReturn(Flux.just(active));
    when(paths.findById(7L)).thenReturn(Mono.just(path));
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(useCase.progressOf(5L))
      .assertNext(p -> {
        assertEquals(List.of(1L), p.completedCourseIds());
        assertEquals(2L, p.currentCourseId());
        assertEquals(50, p.overallPercentage());
        assertEquals(PathEnrollment.ACTIVE, p.enrollment().status());
      })
      .verifyComplete();

    c2.setStatus("completed");
    StepVerifier.create(useCase.progressOf(5L))
      .assertNext(p -> {
        assertEquals(100, p.overallPercentage());
        assertEquals("completed", p.enrollment().status());
      })
      .verifyComplete();
  }

  @Test
  void enrollingIsIdempotentAndSkipsDuplicates() {
    PathEnrollment existing = new PathEnrollment(1L, 7L, 5L, PathEnrollment.ACTIVE, LocalDateTime.now(), null);
    when(gateway.find(7L, 5L)).thenReturn(Mono.just(existing));
    when(gateway.find(7L, 6L)).thenReturn(Mono.empty());
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(useCase.enroll(7L, Arrays.asList(5L, 6L, 6L, null)))
      .assertNext(e -> assertEquals(1L, e.id()))
      .assertNext(e -> {
        assertEquals(6L, e.studentId());
        assertEquals(PathEnrollment.ACTIVE, e.status());
      })
      .verifyComplete();
    verify(gateway, times(1)).save(any());
  }

  @Test
  void minimumScoreRequiresGradedAssessmentsAndUsesTheBestQuizAttempt() {
    var course = TestData.course(1L, "A", null, "published", "inst-1", LocalDateTime.now(), LocalDateTime.now(),
      List.of(new Module(1L, 1L, "M", 1, List.of(new Lesson(100L, 1L, "L", 1,
        List.of(new Content(10L, 100L, "quiz", "{}", 1)))))));
    LearningPath path = new LearningPath(7L, "Ruta", null, "inst-1", LocalDateTime.now(), List.of(course));
    path.setSteps(List.of(new LearningPathStep(1L, true, 70)));
    var pe = new PathEnrollment(1L, 7L, 5L, PathEnrollment.ACTIVE, LocalDateTime.now(), null);
    var enrollment = TestData.enrollment(10L, 5L, 1L, LocalDateTime.now(), 100, LocalDateTime.now());
    enrollment.setStatus("completed");
    when(enrollments.findByStudentId(5L)).thenReturn(Flux.just(enrollment));
    when(gateway.findByStudent(5L)).thenReturn(Flux.just(pe));
    when(paths.findById(7L)).thenReturn(Mono.just(path));
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(useCase.progressOf(5L)).assertNext(p -> assertEquals(0, p.overallPercentage())).verifyComplete();
    when(activity.findAttemptsByStudent(5L)).thenReturn(Flux.just(attempt(60)));
    StepVerifier.create(useCase.progressOf(5L)).assertNext(p -> assertEquals(0, p.overallPercentage())).verifyComplete();
    when(activity.findAttemptsByStudent(5L)).thenReturn(Flux.just(attempt(80), attempt(40)));
    StepVerifier.create(useCase.progressOf(5L)).assertNext(p -> {
      assertEquals(100, p.overallPercentage());
      assertEquals("completed", p.enrollment().status());
    }).verifyComplete();
  }

  private QuizAttempt attempt(int score) {
    return new QuizAttempt(null, 10L, 5L, 1L, 10L, 100L, 1, "{}", score, score >= 70, "{}", LocalDateTime.now());
  }
}
