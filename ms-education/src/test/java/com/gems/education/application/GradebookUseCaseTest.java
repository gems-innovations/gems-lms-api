package com.gems.education.application;

import com.gems.education.application.GradebookUseCase.Cell;
import com.gems.education.application.GradebookUseCase.Row;
import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.ContentBlockGateway.GradableItem;
import com.gems.education.application.gateway.ContentBlockGateway.RubricCriterion;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.GradebookGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.QuizAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class GradebookUseCaseTest {
  private final ContentBlockGateway blocks = mock(ContentBlockGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final CourseActivityGateway activity = mock(CourseActivityGateway.class);
  private final GradebookGateway weights = mock(GradebookGateway.class);
  private final GradebookUseCase useCase = new GradebookUseCase(blocks, enrollments, activity, weights);

  @BeforeEach
  void setUp() {
    when(blocks.gradableItems(1L)).thenReturn(Flux.just(
      new GradableItem(10L, 100L, "quiz", "Quiz 1"),
      new GradableItem(20L, 100L, "assignment", "Proyecto")));
    when(weights.weights(1L)).thenReturn(Mono.just(Map.of(20L, 3)));
    when(enrollments.findByCourseId(1L)).thenReturn(Flux.just(enrollment(50L, 5L), enrollment(60L, 6L)));
    when(activity.findAttemptsByCourse(1L)).thenReturn(Flux.just(attempt(5L, 10L, 40), attempt(5L, 10L, 80)));
    when(activity.findSubmissionsByCourse(1L)).thenReturn(Flux.just(
      submission(1L, 5L, 20L, 60, AssignmentSubmission.GRADED), submission(2L, 6L, 20L, null, AssignmentSubmission.PENDING)));
  }

  @Test
  void weightsTheBestAttemptAndGradedAssignments() {
    StepVerifier.create(useCase.course(1L)).assertNext(book -> {
      assertThat(book.items()).extracting(GradebookUseCase.Item::weight).containsExactly(1, 3);
      Row ana = book.rows().get(0);
      assertThat(ana.cells()).extracting(Cell::score).containsExactly(80, 60);
      assertThat(ana.cells().get(0).attempts()).isEqualTo(2);
      assertThat(ana.currentGrade()).isEqualTo(65.0); // (80*1 + 60*3) / 4
      assertThat(ana.finalGrade()).isEqualTo(65.0);

      Row luis = book.rows().get(1);
      assertThat(luis.cells()).extracting(Cell::state).containsExactly(Cell.MISSING, Cell.PENDING);
      assertThat(luis.cells().get(1).submissionId()).isEqualTo(2L);
      assertThat(luis.currentGrade()).isNull();
      assertThat(luis.finalGrade()).isEqualTo(0.0);
    }).verifyComplete();
  }

  @Test
  void aStudentSeesOnlyTheirRow() {
    when(enrollments.findByStudentIdAndCourseId(6L, 1L)).thenReturn(Mono.just(enrollment(60L, 6L)));
    when(activity.findAttemptsByStudent(6L)).thenReturn(Flux.just(attempt(6L, 10L, 100)));
    when(activity.findSubmissionsByStudent(6L)).thenReturn(Flux.empty());

    StepVerifier.create(useCase.student(1L, 6L)).assertNext(book -> {
      assertThat(book.rows()).hasSize(1);
      assertThat(book.rows().get(0).currentGrade()).isEqualTo(100.0);
      assertThat(book.rows().get(0).finalGrade()).isEqualTo(25.0);
    }).verifyComplete();
  }

  @Test
  void weightsMustBelongToTheCourseAndStayInRange() {
    StepVerifier.create(useCase.saveWeights(1L, Map.of(99L, 1))).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.saveWeights(1L, Map.of(10L, 101))).expectError(IllegalArgumentException.class).verify();
    verify(weights, never()).saveWeights(anyLong(), any());

    when(weights.saveWeights(1L, Map.of(10L, 0))).thenReturn(Mono.empty());
    StepVerifier.create(useCase.saveWeights(1L, Map.of(10L, 0))).expectNextCount(1).verifyComplete();
  }

  @Test
  void rubricGradeIsThePercentageOfThePointsEarned() {
    List<RubricCriterion> rubric = List.of(new RubricCriterion("r1", "Contenido", 60), new RubricCriterion("r2", "Forma", 40));
    assertThat(GradeSubmissionUseCase.rubricGrade(rubric, List.of(score("r1", 45), score("r2", 30)))).isEqualTo(75);

    assertThatThrownBy(() -> GradeSubmissionUseCase.rubricGrade(rubric, List.of(score("r1", 45))))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GradeSubmissionUseCase.rubricGrade(rubric, List.of(score("r1", 61), score("r2", 0))))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GradeSubmissionUseCase.rubricGrade(rubric, List.of(score("r1", 1), score("r2", 1), score("x", 1))))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GradeSubmissionUseCase.rubricGrade(List.of(), List.of()))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void gradingWithTheRubricStoresTheScores() {
    GradeSubmissionUseCase grading = new GradeSubmissionUseCase(activity, blocks);
    when(activity.findSubmission(1L)).thenReturn(Mono.just(submission(1L, 5L, 20L, null, AssignmentSubmission.PENDING)));
    when(blocks.rubric(1L, 20L)).thenReturn(Mono.just(List.of(new RubricCriterion("r1", "Contenido", 10))));
    when(activity.saveSubmission(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(grading.executeWithRubric(1L, List.of(score("r1", 7)), "Bien", "[json]"))
      .assertNext(s -> {
        assertThat(s.grade()).isEqualTo(70);
        assertThat(s.rubricScores()).isEqualTo("[json]");
        assertThat(s.status()).isEqualTo(AssignmentSubmission.GRADED);
      }).verifyComplete();

    StepVerifier.create(grading.execute(1L, 101, null)).expectError(IllegalArgumentException.class).verify();
  }

  private static GradeSubmissionUseCase.RubricScore score(String id, int score) {
    return new GradeSubmissionUseCase.RubricScore(id, score, null);
  }

  private static Enrollment enrollment(Long id, Long studentId) {
    return new Enrollment(id, studentId, 1L, "active", LocalDateTime.now(), 0, null);
  }

  private static QuizAttempt attempt(Long studentId, Long blockId, int score) {
    return new QuizAttempt(null, 50L, studentId, 1L, blockId, 100L, 1, "[]", score, score >= 70, "[]", LocalDateTime.now());
  }

  private static AssignmentSubmission submission(Long id, Long studentId, Long blockId, Integer grade, String status) {
    return new AssignmentSubmission(id, 50L, studentId, 1L, blockId, 100L, "texto", "[]", LocalDateTime.now(),
      grade, null, status, null);
  }
}
