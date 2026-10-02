package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("quiz_attempts")
public class QuizAttemptEntity {
  @Id
  private Long id;
  @Column("enrollment_id")
  private Long enrollmentId;
  @Column("student_id")
  private Long studentId;
  @Column("course_id")
  private Long courseId;
  @Column("block_id")
  private Long blockId;
  @Column("lesson_id")
  private Long lessonId;
  @Column("attempt_number")
  private Integer attemptNumber;
  private String answers;
  private Integer score;
  private Boolean passed;
  private String feedback;
  @Column("completed_at")
  private LocalDateTime completedAt;

  public QuizAttemptEntity() {
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEnrollmentId() { return enrollmentId; }
  public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }
  public Long getStudentId() { return studentId; }
  public void setStudentId(Long studentId) { this.studentId = studentId; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public Long getBlockId() { return blockId; }
  public void setBlockId(Long blockId) { this.blockId = blockId; }
  public Long getLessonId() { return lessonId; }
  public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
  public Integer getAttemptNumber() { return attemptNumber; }
  public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
  public String getAnswers() { return answers; }
  public void setAnswers(String answers) { this.answers = answers; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
  public Boolean getPassed() { return passed; }
  public void setPassed(Boolean passed) { this.passed = passed; }
  public String getFeedback() { return feedback; }
  public void setFeedback(String feedback) { this.feedback = feedback; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
