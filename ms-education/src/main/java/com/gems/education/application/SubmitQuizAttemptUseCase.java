package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.ContentBlockGateway.BlockInfo;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.QuizComposer;
import com.gems.education.application.gateway.QuizSessionGateway;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.QuizAttempt;
import com.gems.education.domain.entities.QuizSession;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.gems.education.application.exceptions.CourseActivityException.*;

/**
 * Quiz attempts, graded on the server. A quiz that draws from the question bank, is timed or
 * shuffled starts with a session ({@link #start}) that fixes the questions and the deadline; the
 * attempt is then graded against that session. Other quizzes may still be answered directly.
 */
public class SubmitQuizAttemptUseCase {
  private static final String QUIZ = "quiz";
  /** Allowance for network delay after the deadline. Later answers are not graded. */
  public static final long GRACE_SECONDS = 30;
  private static final String NO_ANSWERS = "[]";

  private final EnrollmentGateway enrollmentGateway;
  private final CourseActivityGateway activityGateway;
  private final ContentBlockGateway contentBlockGateway;
  private final QuizComposer composer;
  private final QuizSessionGateway sessions;
  private final Clock clock;

  public SubmitQuizAttemptUseCase(EnrollmentGateway enrollmentGateway, CourseActivityGateway activityGateway,
                                  ContentBlockGateway contentBlockGateway, QuizComposer composer,
                                  QuizSessionGateway sessions, Clock clock) {
    this.enrollmentGateway = enrollmentGateway;
    this.activityGateway = activityGateway;
    this.contentBlockGateway = contentBlockGateway;
    this.composer = composer;
    this.sessions = sessions;
    this.clock = clock;
  }

  /**
   * Starts (or resumes) an attempt. An unsubmitted session that is still on time is returned as
   * is, so reloading neither redraws the questions nor resets the timer. One whose time ran out
   * is recorded as an attempt without answers before a new one starts.
   */
  public Mono<QuizSession> start(Long studentId, Long courseId, Long blockId) {
    return quizContext(studentId, courseId, blockId).flatMap(ctx ->
      sessions.findOpen(ctx.enrollment().getId(), blockId)
        .flatMap(open -> {
          if (!open.expired(now(), GRACE_SECONDS)) return Mono.just(open);
          return record(ctx, open, NO_ANSWERS).then(Mono.<QuizSession>empty());
        })
        .switchIfEmpty(Mono.defer(() -> requireAttemptsLeft(ctx)
          .then(composer.compose(courseId, blockId))
          .flatMap(quiz -> {
            LocalDateTime started = now();
            return sessions.save(new QuizSession(null, ctx.enrollment().getId(), studentId, courseId, blockId,
              quiz.questions(), quiz.studentQuestions(), quiz.passingScore(), started,
              quiz.timeLimitSeconds() > 0 ? started.plusSeconds(quiz.timeLimitSeconds()) : null, null));
          }))));
  }

  /** Answers to a quiz without a session (untimed quizzes made only of their own questions). */
  public Mono<QuizAttempt> execute(Long studentId, Long courseId, Long blockId, String answers) {
    return quizContext(studentId, courseId, blockId).flatMap(ctx -> {
      if (ctx.block().sessionRequired()) {
        return Mono.error(new CourseActivityException(SESSION_REQUIRED, "Start the quiz before answering it"));
      }
      return requireAttemptsLeft(ctx).flatMap(previous -> contentBlockGateway.grade(courseId, blockId, answers)
        .flatMap(grading -> save(ctx, previous, answers, grading)));
    });
  }

  /** Answers to a started session. Past the deadline (plus grace) the answers are not graded. */
  public Mono<QuizAttempt> execute(Long studentId, Long courseId, Long blockId, Long sessionId, String answers) {
    return quizContext(studentId, courseId, blockId).flatMap(ctx -> sessions.findById(sessionId)
      .filter(s -> Objects.equals(s.enrollmentId(), ctx.enrollment().getId()) && Objects.equals(s.blockId(), blockId))
      .switchIfEmpty(Mono.error(new CourseActivityException(SESSION_NOT_FOUND, "Quiz session not found")))
      .flatMap(session -> {
        if (session.submittedAt() != null) {
          return Mono.error(new CourseActivityException(SESSION_CLOSED, "This attempt was already submitted"));
        }
        return record(ctx, session, session.expired(now(), GRACE_SECONDS) ? NO_ANSWERS : answers);
      }));
  }

  /** Closes the session and stores its attempt; a concurrent submit of the same session fails. */
  private Mono<QuizAttempt> record(Context ctx, QuizSession session, String answers) {
    return sessions.close(session.id())
      .switchIfEmpty(Mono.error(new CourseActivityException(SESSION_CLOSED, "This attempt was already submitted")))
      .flatMap(closed -> activityGateway.countAttempts(ctx.enrollment().getId(), session.blockId()))
      .flatMap(previous -> save(ctx, previous, answers,
        composer.grade(session.questions(), session.passingScore(), answers)));
  }

  private Mono<QuizAttempt> save(Context ctx, Long previous, String answers, ContentBlockGateway.Grading grading) {
    Enrollment e = ctx.enrollment();
    return activityGateway.saveAttempt(new QuizAttempt(null, e.getId(), e.getStudentId(), e.getCourseId(),
      ctx.blockId(), ctx.block().lessonId(), previous.intValue() + 1, answers, grading.score(), grading.passed(),
      grading.feedback(), now()));
  }

  private Mono<Long> requireAttemptsLeft(Context ctx) {
    return activityGateway.countAttempts(ctx.enrollment().getId(), ctx.blockId()).flatMap(previous -> {
      if (ctx.block().maxAttempts() > 0 && previous >= ctx.block().maxAttempts()) {
        return Mono.error(new CourseActivityException(ATTEMPT_LIMIT_REACHED, "No attempts left for this quiz"));
      }
      return Mono.just(previous);
    });
  }

  private Mono<Context> quizContext(Long studentId, Long courseId, Long blockId) {
    return enrollmentGateway.findByStudentIdAndCourseId(studentId, courseId)
      .switchIfEmpty(Mono.error(new CourseActivityException(NOT_ENROLLED, "You are not enrolled in this course")))
      .flatMap(enrollment -> contentBlockGateway.find(courseId, blockId)
        .switchIfEmpty(Mono.error(new CourseActivityException(BLOCK_NOT_FOUND, "Content block not found in this course")))
        .flatMap(block -> QUIZ.equals(block.type())
          ? Mono.just(new Context(enrollment, blockId, block))
          : Mono.error(new CourseActivityException(WRONG_BLOCK_TYPE, "This content block is not a quiz"))));
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock);
  }

  private record Context(Enrollment enrollment, Long blockId, BlockInfo block) {
  }
}
