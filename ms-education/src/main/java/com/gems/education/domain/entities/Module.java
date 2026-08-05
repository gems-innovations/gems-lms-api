package com.gems.education.domain.entities;

import java.util.List;

public class Module {
  private Long id;
  private Long courseId;
  private String title;
  private Integer orderIndex;
  private List<Lesson> lessons;

  public Module() {
  }

  public Module(Long id, Long courseId, String title, Integer orderIndex, List<Lesson> lessons) {
    this.id = id;
    this.courseId = courseId;
    this.title = title;
    this.orderIndex = orderIndex;
    this.lessons = lessons;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
  }

  public List<Lesson> getLessons() {
    return lessons;
  }

  public void setLessons(List<Lesson> lessons) {
    this.lessons = lessons;
  }
}
