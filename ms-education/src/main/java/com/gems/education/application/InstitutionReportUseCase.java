package com.gems.education.application;

import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Enrollment;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Builds an institution-wide operational report from the academic source of truth. */
public class InstitutionReportUseCase {
  private final CourseGateway courses;
  private final EnrollmentGateway enrollments;
  private final CourseActivityGateway activity;

  public InstitutionReportUseCase(CourseGateway courses, EnrollmentGateway enrollments, CourseActivityGateway activity) {
    this.courses = courses;
    this.enrollments = enrollments;
    this.activity = activity;
  }

  public Mono<InstitutionReport> execute(String institutionId) {
    return Mono.zip(courses.findByInstitutionId(institutionId).collectList(),
        enrollments.findByInstitutionId(institutionId).collectList(),
        activity.findSubmissionsByInstitution(institutionId).collectList())
      .map(values -> report(institutionId, values.getT1(), values.getT2(), values.getT3()));
  }

  private InstitutionReport report(String institutionId, List<Course> courses, List<Enrollment> enrollments,
                                   List<AssignmentSubmission> submissions) {
    Map<Long, List<Enrollment>> enrollmentByCourse = enrollments.stream()
      .collect(Collectors.groupingBy(Enrollment::getCourseId));
    Map<Long, List<AssignmentSubmission>> submissionsByCourse = submissions.stream()
      .collect(Collectors.groupingBy(AssignmentSubmission::courseId));
    List<CourseReport> courseReports = courses.stream().map(course -> {
      List<Enrollment> inCourse = enrollmentByCourse.getOrDefault(course.getId(), List.of());
      int completed = (int) inCourse.stream().filter(this::completed).count();
      int active = (int) inCourse.stream().filter(this::active).count();
      int pending = (int) submissionsByCourse.getOrDefault(course.getId(), List.of()).stream()
        .filter(s -> AssignmentSubmission.PENDING.equals(s.status())).count();
      return new CourseReport(course.getId(), course.getTitle(), course.getStatus(), inCourse.size(), active,
        completed, percentage(completed, inCourse.size()), averageProgress(inCourse), pending,
        course.getAverageRating(), course.getRatingCount());
    }).toList();
    int completed = (int) enrollments.stream().filter(this::completed).count();
    int active = (int) enrollments.stream().filter(this::active).count();
    int pending = (int) submissions.stream().filter(s -> AssignmentSubmission.PENDING.equals(s.status())).count();
    long students = enrollments.stream().map(Enrollment::getStudentId).filter(Objects::nonNull).distinct().count();
    return new InstitutionReport(institutionId, OffsetDateTime.now().toString(), courses.size(), enrollments.size(),
      active, completed, (int) students, percentage(completed, enrollments.size()), averageProgress(enrollments),
      pending, courseReports);
  }

  private boolean completed(Enrollment enrollment) { return "completed".equalsIgnoreCase(enrollment.getStatus()); }
  private boolean active(Enrollment enrollment) { return "active".equalsIgnoreCase(enrollment.getStatus()); }
  private static double percentage(int value, int total) { return total == 0 ? 0 : rounded(value * 100.0 / total); }
  private static double averageProgress(List<Enrollment> values) {
    return values.isEmpty() ? 0 : rounded(values.stream().map(Enrollment::getProgress).filter(Objects::nonNull)
      .mapToInt(Integer::intValue).average().orElse(0));
  }
  private static double rounded(double value) { return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue(); }

  public record InstitutionReport(String institutionId, String generatedAt, int totalCourses, int totalEnrollments,
                                  int activeEnrollments, int completedEnrollments, int activeStudents,
                                  double completionRate, double averageProgress, int pendingSubmissions,
                                  List<CourseReport> courses) { }
  public record CourseReport(Long courseId, String title, String status, int enrollments, int activeEnrollments,
                             int completedEnrollments, double completionRate, double averageProgress,
                             int pendingSubmissions, BigDecimal averageRating, Integer ratingCount) { }
}
