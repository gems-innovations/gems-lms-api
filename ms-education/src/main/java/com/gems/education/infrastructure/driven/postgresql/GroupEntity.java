package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("student_groups")
public class GroupEntity {
  @Id
  private Long id;
  private String name;
  @Column("institution_id")
  private String institutionId;
  @Column("instructor_id")
  private Long instructorId;
  @Column("student_ids")
  private Long[] studentIds;
  @Column("course_ids")
  private Long[] courseIds;
  @Column("path_ids")
  private Long[] pathIds;
  @Column("created_at")
  private LocalDateTime createdAt;

  public GroupEntity() {
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getInstitutionId() { return institutionId; }
  public void setInstitutionId(String institutionId) { this.institutionId = institutionId; }
  public Long getInstructorId() { return instructorId; }
  public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }
  public Long[] getStudentIds() { return studentIds; }
  public void setStudentIds(Long[] studentIds) { this.studentIds = studentIds; }
  public Long[] getCourseIds() { return courseIds; }
  public void setCourseIds(Long[] courseIds) { this.courseIds = courseIds; }
  public Long[] getPathIds() { return pathIds; }
  public void setPathIds(Long[] pathIds) { this.pathIds = pathIds; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
