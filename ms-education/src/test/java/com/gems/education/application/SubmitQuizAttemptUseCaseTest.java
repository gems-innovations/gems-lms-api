package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.ContentBlockGateway.BlockInfo;
import com.gems.education.application.gateway.ContentBlockGateway.Grading;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.QuizComposer;
import com.gems.education.application.gateway.QuizComposer.ComposedQuiz;
import com.gems.education.application.gateway.QuizSessionGateway;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.QuizAttempt;
import com.gems.education.domain.entities.QuizSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubmitQuizAttemptUseCaseTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);

  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final CourseActivityGateway activity = mock(CourseActivityGateway.class);
  private final ContentBlockGateway blocks = mock(ContentBlockGateway.class);
  private final QuizComposer composer = mock(QuizComposer.class);
  private final QuizSessionGateway sessions = mock(QuizSessionGateway.class);
  private final Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  private final SubmitQuizAttemptUseCase useCase =
    new SubmitQuizAttemptUseCase(enrollments, activity, blocks, composer, sessions, clock);

  @BeforeEach
  void setUp() {
    when(enrollments.findByStudentIdAndCourseId(5L, 1L))
      .thenReturn(Mono.just(new Enrollment(50L, 5L, 1L, "active", NOW, 0, null)));
    when(blocks.find(1L, 7L)).thenReturn(Mono.just(new BlockInfo(100L, "quiz", 2, true)));
    when(activity.countAttempts(50L, 7L)).thenReturn(Mono.just(0L));
    when(activity.saveAttempt(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(sessions.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 9L)));
    when(sessions.close(anyLong())).thenAnswer(inv -> Mono.just(session(inv.getArgument(0), NOW.plusMinutes(10))));
    when(composer.compose(1L, 7L)).thenReturn(Mono.just(new ComposedQuiz("[full]", "[visible]", 70, 600)));
    when(composer.grade(any(), anyInt(), any())).thenReturn(new Grading(80, true, "[]"));
  }

  @Test
  void startsATimedSessionWithTheComposedQuestions() {
    when(sessions.findOpen(50L, 7L)).thenReturn(Mono.empty());

    StepVerifier.create(useCase.start(5L, 1L, 7L)).assertNext(s -> {
      assertThat(s.id()).isEqualTo(9L);
      assertThat(s.studentQuestions()).isEqualTo("[visible]");
      assertThat(s.expiresAt()).isEqualTo(NOW.plusMinutes(10));
    }).verifyComplete();
  }

  @Test
  void resumesAnOpenSessionInsteadOfRedrawing() {
    when(sessions.findOpen(50L, 7L)).thenReturn(Mono.just(session(3L, NOW.plusMinutes(5))));

    StepVerifier.create(useCase.start(5L, 1L, 7L)).assertNext(s -> assertThat(s.id()).isEqualTo(3L)).verifyComplete();
    verify(composer, never()).compose(anyLong(), anyLong());
  }

  @Test
  void anExpiredOpenSessionCountsAsAnAttemptWithoutAnswers() {
    when(sessions.findOpen(50L, 7L)).thenReturn(Mono.just(session(3L, NOW.minusMinutes(5))));

    StepVerifier.create(useCase.start(5L, 1L, 7L)).assertNext(s -> assertThat(s.id()).isEqualTo(9L)).verifyComplete();
    verify(composer).grade("[full]", 70, "[]");
    verify(activity).saveAttempt(any());
  }

  @Test
  void noNewSessionWhenTheAttemptsAreUsed() {
    when(sessions.findOpen(50L, 7L)).thenReturn(Mono.empty());
    when(activity.countAttempts(50L, 7L)).thenReturn(Mono.just(2L));

    StepVerifier.create(useCase.start(5L, 1L, 7L))
      .expectErrorMatches(e -> e instanceof CourseActivityException c && c.getCode().equals(CourseActivityException.ATTEMPT_LIMIT_REACHED))
      .verify();
  }

  @Test
  void gradesAgainstTheSessionAndIgnoresLateAnswers() {
    when(sessions.findById(3L)).thenReturn(Mono.just(session(3L, NOW.plusMinutes(1))));
    StepVerifier.create(useCase.execute(5L, 1L, 7L, 3L, "[answers]"))
      .assertNext(a -> assertThat(a.score()).isEqualTo(80)).verifyComplete();
    verify(composer).grade("[full]", 70, "[answers]");

    // 31 seconds past the deadline: beyond the grace period.
    when(sessions.findById(4L)).thenReturn(Mono.just(session(4L, NOW.minusSeconds(SubmitQuizAttemptUseCase.GRACE_SECONDS + 1))));
    StepVerifier.create(useCase.execute(5L, 1L, 7L, 4L, "[late]")).expectNextCount(1).verifyComplete();
    ArgumentCaptor<QuizAttempt> saved = ArgumentCaptor.forClass(QuizAttempt.class);
    verify(activity, times(2)).saveAttempt(saved.capture());
    assertThat(saved.getAllValues().get(1).answers()).isEqualTo("[]");
  }

  @Test
  void rejectsSessionsOfOthersAndDoubleSubmits() {
    QuizSession foreign = new QuizSession(3L, 99L, 6L, 1L, 7L, "[full]", "[visible]", 70, NOW, null, null);
    when(sessions.findById(3L)).thenReturn(Mono.just(foreign));
    StepVerifier.create(useCase.execute(5L, 1L, 7L, 3L, "[]"))
      .expectErrorMatches(e -> e instanceof CourseActivityException c && c.getCode().equals(CourseActivityException.SESSION_NOT_FOUND))
      .verify();

    when(sessions.findById(4L)).thenReturn(Mono.just(session(4L, null)));
    when(sessions.close(4L)).thenReturn(Mono.empty());
    StepVerifier.create(useCase.execute(5L, 1L, 7L, 4L, "[]"))
      .expectErrorMatches(e -> e instanceof CourseActivityException c && c.getCode().equals(CourseActivityException.SESSION_CLOSED))
      .verify();
  }

  @Test
  void quizzesThatNeedASessionCannotBeAnsweredDirectly() {
    StepVerifier.create(useCase.execute(5L, 1L, 7L, "[]"))
      .expectErrorMatches(e -> e instanceof CourseActivityException c && c.getCode().equals(CourseActivityException.SESSION_REQUIRED))
      .verify();

    when(blocks.find(1L, 7L)).thenReturn(Mono.just(new BlockInfo(100L, "quiz", 0, false)));
    when(blocks.grade(1L, 7L, "[]")).thenReturn(Mono.just(new Grading(100, true, "[]")));
    StepVerifier.create(useCase.execute(5L, 1L, 7L, "[]"))
      .assertNext(a -> assertThat(a.attemptNumber()).isEqualTo(1)).verifyComplete();
  }

  private static QuizSession session(Long id, LocalDateTime expiresAt) {
    return new QuizSession(id, 50L, 5L, 1L, 7L, "[full]", "[visible]", 70, NOW.minusMinutes(1), expiresAt, null);
  }

  private static QuizSession withId(QuizSession s, Long id) {
    return new QuizSession(id, s.enrollmentId(), s.studentId(), s.courseId(), s.blockId(), s.questions(),
      s.studentQuestions(), s.passingScore(), s.startedAt(), s.expiresAt(), s.submittedAt());
  }
}
