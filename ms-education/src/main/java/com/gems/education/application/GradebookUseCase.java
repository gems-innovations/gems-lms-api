package com.gems.education.application;

import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.ContentBlockGateway.GradableItem;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.GradebookGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.QuizAttempt;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Course gradebook: one column per quiz or assignment block and one row per enrolled student.
 * A quiz counts its best attempt; an assignment its grade once graded. The course grade is the
 * weighted average of the columns (weights default to 1; weight 0 leaves a column out).
 */
public class GradebookUseCase {
  public static final int MAX_WEIGHT = 100;

  private final ContentBlockGateway contentBlockGateway;
  private final EnrollmentGateway enrollmentGateway;
  private final CourseActivityGateway activityGateway;
  private final GradebookGateway gradebookGateway;

  public GradebookUseCase(ContentBlockGateway contentBlockGateway, EnrollmentGateway enrollmentGateway,
                          CourseActivityGateway activityGateway, GradebookGateway gradebookGateway) {
    this.contentBlockGateway = contentBlockGateway;
    this.enrollmentGateway = enrollmentGateway;
    this.activityGateway = activityGateway;
    this.gradebookGateway = gradebookGateway;
  }

  /** Every enrolled student of the course. */
  public Mono<Gradebook> course(Long courseId) {
    return build(courseId, enrollmentGateway.findByCourseId(courseId).collectList(),
      activityGateway.findAttemptsByCourse(courseId).collectList(),
      activityGateway.findSubmissionsByCourse(courseId).collectList());
  }

  /** Only the given student's row; empty rows when they are not enrolled. */
  public Mono<Gradebook> student(Long courseId, Long studentId) {
    return build(courseId,
      enrollmentGateway.findByStudentIdAndCourseId(studentId, courseId).map(List::of).defaultIfEmpty(List.of()),
      activityGateway.findAttemptsByStudent(studentId).filter(a -> courseId.equals(a.courseId())).collectList(),
      activityGateway.findSubmissionsByStudent(studentId).filter(s -> courseId.equals(s.courseId())).collectList());
  }

  /** Replaces the weights; every block must be a gradable block of the course. */
  public Mono<Gradebook> saveWeights(Long courseId, Map<Long, Integer> weights) {
    return contentBlockGateway.gradableItems(courseId).map(GradableItem::blockId).collect(HashSet<Long>::new, Set::add)
      .flatMap(blocks -> {
        for (Map.Entry<Long, Integer> w : weights.entrySet()) {
          if (!blocks.contains(w.getKey())) {
            return Mono.error(new IllegalArgumentException("Block " + w.getKey() + " is not a quiz or assignment of the course"));
          }
          if (w.getValue() == null || w.getValue() < 0 || w.getValue() > MAX_WEIGHT) {
            return Mono.error(new IllegalArgumentException("Weights must be between 0 and " + MAX_WEIGHT));
          }
        }
        return gradebookGateway.saveWeights(courseId, weights);
      })
      .then(Mono.defer(() -> course(courseId)));
  }

  private Mono<Gradebook> build(Long courseId, Mono<List<Enrollment>> enrollments, Mono<List<QuizAttempt>> attempts,
                                Mono<List<AssignmentSubmission>> submissions) {
    return Mono.zip(contentBlockGateway.gradableItems(courseId).collectList(),
        gradebookGateway.weights(courseId).defaultIfEmpty(Map.of()), enrollments, attempts, submissions)
      .map(t -> {
        List<Item> items = t.getT1().stream()
          .map(i -> new Item(i.blockId(), i.lessonId(), i.type(), i.title(), t.getT2().getOrDefault(i.blockId(), 1)))
          .toList();
        List<Row> rows = t.getT3().stream().map(e -> row(e, items, t.getT4(), t.getT5())).toList();
        return new Gradebook(courseId, items, rows);
      });
  }

  static Row row(Enrollment enrollment, List<Item> items, List<QuizAttempt> attempts,
                 List<AssignmentSubmission> submissions) {
    Map<Long, Integer> best = new HashMap<>();
    Map<Long, Integer> attemptCount = new HashMap<>();
    attempts.stream().filter(a -> Objects.equals(a.studentId(), enrollment.getStudentId())).forEach(a -> {
      best.merge(a.blockId(), a.score(), Math::max);
      attemptCount.merge(a.blockId(), 1, Integer::sum);
    });
    Map<Long, AssignmentSubmission> delivered = new LinkedHashMap<>();
    submissions.stream().filter(s -> Objects.equals(s.studentId(), enrollment.getStudentId()))
      .forEach(s -> delivered.put(s.blockId(), s));

    List<Cell> cells = new ArrayList<>();
    double weighted = 0;
    int scoredWeight = 0;
    int totalWeight = 0;
    for (Item item : items) {
      Integer score;
      String state;
      Long submissionId = null;
      if ("quiz".equals(item.type())) {
        score = best.get(item.blockId());
        state = score != null ? Cell.GRADED : Cell.MISSING;
      } else {
        AssignmentSubmission s = delivered.get(item.blockId());
        submissionId = s != null ? s.id() : null;
        score = s != null && AssignmentSubmission.GRADED.equals(s.status()) ? s.grade() : null;
        state = s == null ? Cell.MISSING : score != null ? Cell.GRADED : Cell.PENDING;
      }
      cells.add(new Cell(item.blockId(), score, state, attemptCount.getOrDefault(item.blockId(), 0), submissionId));
      totalWeight += item.weight();
      if (score != null) {
        weighted += (double) score * item.weight();
        scoredWeight += item.weight();
      }
    }
    return new Row(enrollment.getStudentId(), enrollment.getId(), enrollment.getStatus(), cells,
      scoredWeight > 0 ? round(weighted / scoredWeight) : null,
      totalWeight > 0 ? round(weighted / totalWeight) : null);
  }

  private static Double round(double value) {
    return Math.round(value * 10) / 10.0;
  }

  public record Gradebook(Long courseId, List<Item> items, List<Row> rows) {
  }

  public record Item(Long blockId, Long lessonId, String type, String title, int weight) {
  }

  /**
   * currentGrade averages only what is already graded; finalGrade counts missing and pending work
   * as 0. Both are null when there is nothing to average.
   */
  public record Row(Long studentId, Long enrollmentId, String enrollmentStatus, List<Cell> cells,
                    Double currentGrade, Double finalGrade) {
  }

  /** state: graded, pending (delivered, not graded yet) or missing. */
  public record Cell(Long blockId, Integer score, String state, int attempts, Long submissionId) {
    public static final String GRADED = "graded";
    public static final String PENDING = "pending";
    public static final String MISSING = "missing";
  }
}
