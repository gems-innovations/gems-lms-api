package com.gems.education;

import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.command.ModuleCommand;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.Module;

import java.time.LocalDateTime;
import java.util.List;

/** Short factories for test fixtures; fields a test does not care about get neutral defaults. */
public final class TestData {

  private TestData() {
  }

  public static Course course(Long id, String title, String description, String status, String institutionId,
                              LocalDateTime createdAt, LocalDateTime updatedAt, List<Module> modules) {
    int lessons = modules.stream().mapToInt(m -> m.getLessons() == null ? 0 : m.getLessons().size()).sum();
    return new Course(id, title, description, status, "beginner", List.of(), null, null, institutionId,
      0, lessons, 0, 0, null, 0, null, createdAt, updatedAt, modules);
  }

  public static CourseCommand courseCommand(String title, String description, String status, String institutionId,
                                            List<ModuleCommand> modules) {
    return new CourseCommand(title, description, status, "beginner", List.of(), null, null, institutionId, modules);
  }

  public static Enrollment enrollment(Long id, Long studentId, Long courseId, LocalDateTime enrolledAt,
                                      Integer progress, LocalDateTime completedAt) {
    return new Enrollment(id, studentId, courseId, "active", enrolledAt, progress, completedAt);
  }

  public static EnrollmentResponse enrollmentResponse(Long id, Long studentId, Long courseId,
                                                      LocalDateTime enrolledAt, Integer progress,
                                                      LocalDateTime completedAt) {
    return new EnrollmentResponse(id, studentId, courseId, "active", enrolledAt, progress, completedAt);
  }
}
