package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ContentRequest {
  @NotBlank(message = "Content type is required")
  private String type;

  @NotBlank(message = "Content value is required")
  private String value;

  @NotNull(message = "Content order index is required")
  private Integer orderIndex;

  public ContentRequest() {
  }

  public ContentRequest(String type, String value, Integer orderIndex) {
    this.type = type;
    this.value = value;
    this.orderIndex = orderIndex;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
  }
}
