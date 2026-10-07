package com.gems.education.domain.entities;

import java.util.List;

public class Lesson {
  private Long id;
  private Long moduleId;
  private String title;
  private Integer orderIndex;
  private List<Content> contents;

  public Lesson() {
  }

  public Lesson(Long id, Long moduleId, String title, Integer orderIndex, List<Content> contents) {
    this.id = id;
    this.moduleId = moduleId;
    this.title = title;
    this.orderIndex = orderIndex;
    this.contents = contents;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getModuleId() {
    return moduleId;
  }

  public void setModuleId(Long moduleId) {
    this.moduleId = moduleId;
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

  public List<Content> getContents() {
    return contents;
  }

  public void setContents(List<Content> contents) {
    this.contents = contents;
  }

  /** Optional lesson description; isFree lessons can be previewed without enrolling. */
  private String description;
  private Boolean isFree = false;

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Boolean getIsFree() { return isFree; }
  public void setIsFree(Boolean isFree) { this.isFree = Boolean.TRUE.equals(isFree); }

  public Lesson details(String description, Boolean isFree) {
    this.description = description;
    this.isFree = Boolean.TRUE.equals(isFree);
    return this;
  }
}
