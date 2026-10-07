package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class EnrollmentProgressRequest {
  @Min(value = 0, message = "Progress must be between 0 and 100")
  @Max(value = 100, message = "Progress must be between 0 and 100")
  private Integer progress;

  // Detailed progress (completed blocks, quiz attempts, submissions...) serialized as JSON by the client.
  private String progressData;

  public EnrollmentProgressRequest() {
  }

  public Integer getProgress() {
    return progress;
  }

  public void setProgress(Integer progress) {
    this.progress = progress;
  }

  public String getProgressData() {
    return progressData;
  }

  public void setProgressData(String progressData) {
    this.progressData = progressData;
  }
}
