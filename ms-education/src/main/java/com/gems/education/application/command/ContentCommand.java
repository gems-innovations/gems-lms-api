package com.gems.education.application.command;

public class ContentCommand {
  private final String type;
  private final String value;
  private final Integer orderIndex;

  public ContentCommand(String type, String value, Integer orderIndex) {
    this.type = type;
    this.value = value;
    this.orderIndex = orderIndex;
  }

  public String getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }
}
