package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.ContentBlockGateway.RubricCriterion;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Grades a submission (0-100) directly or from the scores of each criterion of the block rubric. */
public class GradeSubmissionUseCase {
  private final CourseActivityGateway activityGateway;
  private final ContentBlockGateway contentBlockGateway;

  public GradeSubmissionUseCase(CourseActivityGateway activityGateway, ContentBlockGateway contentBlockGateway) {
    this.activityGateway = activityGateway;
    this.contentBlockGateway = contentBlockGateway;
  }

  public Mono<AssignmentSubmission> findSubmission(Long submissionId) {
    return activityGateway.findSubmission(submissionId)
      .switchIfEmpty(Mono.error(new CourseActivityException(
        CourseActivityException.SUBMISSION_NOT_FOUND, "Submission not found with ID " + submissionId)));
  }

  public Mono<AssignmentSubmission> execute(Long submissionId, int grade, String feedback) {
    if (grade < 0 || grade > 100) {
      return Mono.error(new IllegalArgumentException("Grade must be between 0 and 100"));
    }
    return findSubmission(submissionId)
      .flatMap(submission -> activityGateway.saveSubmission(submission.graded(grade, feedback)));
  }

  /**
   * Every criterion of the rubric must be scored once, between 0 and its maximum. The grade is the
   * percentage of the rubric points earned. scoresJson is the same scores, stored as given.
   */
  public Mono<AssignmentSubmission> executeWithRubric(Long submissionId, List<RubricScore> scores, String feedback,
                                                      String scoresJson) {
    return findSubmission(submissionId)
      .flatMap(submission -> contentBlockGateway.rubric(submission.courseId(), submission.blockId())
        .defaultIfEmpty(List.of())
        .flatMap(criteria -> Mono.fromCallable(() -> rubricGrade(criteria, scores)))
        .flatMap(grade -> activityGateway.saveSubmission(submission.graded(grade, feedback, scoresJson))));
  }

  static int rubricGrade(List<RubricCriterion> criteria, List<RubricScore> scores) {
    if (criteria.isEmpty()) throw new IllegalArgumentException("The assignment has no rubric");
    Map<String, Integer> given = new HashMap<>();
    for (RubricScore s : scores == null ? List.<RubricScore>of() : scores) {
      if (s.criterionId() == null || given.put(s.criterionId(), s.score()) != null) {
        throw new IllegalArgumentException("Each rubric criterion must be scored once");
      }
    }
    int earned = 0;
    int possible = 0;
    for (RubricCriterion c : criteria) {
      Integer score = given.remove(c.id());
      if (score == null) throw new IllegalArgumentException("Missing score for criterion " + c.criterion());
      if (score < 0 || score > c.maxPoints()) {
        throw new IllegalArgumentException("Score for " + c.criterion() + " must be between 0 and " + c.maxPoints());
      }
      earned += score;
      possible += c.maxPoints();
    }
    if (!given.isEmpty()) throw new IllegalArgumentException("Unknown rubric criterion " + given.keySet().iterator().next());
    return Math.round(earned * 100f / possible);
  }

  /** score is required: a missing value is rejected instead of counting as 0. */
  public record RubricScore(String criterionId, Integer score, String comment) {
  }
}
