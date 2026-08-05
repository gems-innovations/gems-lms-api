package com.gems.education.application.command;

import java.util.List;

public class LessonCommand {
  private final String title;
  private final Integer orderIndex;
  private final List<ContentCommand> contents;

  public LessonCommand(String title, Integer orderIndex, List<ContentCommand> contents) {
    this.title = title;
    this.orderIndex = orderIndex;
    this.contents = contents;
  }

  public String getTitle() {
    return title;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public List<ContentCommand> getContents() {
    return contents;
  }
}
