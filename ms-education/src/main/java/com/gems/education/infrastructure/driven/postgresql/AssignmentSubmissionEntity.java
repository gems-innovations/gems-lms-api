package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("assignment_submissions")
public class AssignmentSubmissionEntity {
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
  @Column("text_content")
  private String textContent;
  @Column("file_urls")
  private String fileUrls;
  @Column("submitted_at")
  private LocalDateTime submittedAt;
  private Integer grade;
  private String feedback;
  private String status;
  @Column("rubric_scores")
  private String rubricScores;

  public AssignmentSubmissionEntity() {
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
  public String getTextContent() { return textContent; }
  public void setTextContent(String textContent) { this.textContent = textContent; }
  public String getFileUrls() { return fileUrls; }
  public void setFileUrls(String fileUrls) { this.fileUrls = fileUrls; }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
  public Integer getGrade() { return grade; }
  public void setGrade(Integer grade) { this.grade = grade; }
  public String getFeedback() { return feedback; }
  public void setFeedback(String feedback) { this.feedback = feedback; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRubricScores() { return rubricScores; }
  public void setRubricScores(String rubricScores) { this.rubricScores = rubricScores; }
}
