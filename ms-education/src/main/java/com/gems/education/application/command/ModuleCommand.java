package com.gems.education.application.command;

import java.util.List;

public class ModuleCommand {
  private final String title;
  private final Integer orderIndex;
  private final List<LessonCommand> lessons;

  public ModuleCommand(String title, Integer orderIndex, List<LessonCommand> lessons) {
    this.title = title;
    this.orderIndex = orderIndex;
    this.lessons = lessons;
  }

  public String getTitle() {
    return title;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public List<LessonCommand> getLessons() {
    return lessons;
  }
}
