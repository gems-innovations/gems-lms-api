package com.gems.education.application.response;

public class QuizGradingResponse {
  private Integer score;
  private boolean passed;
  private Integer correctCount;
  private Integer totalCount;

  public QuizGradingResponse() {
  }

  public QuizGradingResponse(Integer score, boolean passed, Integer correctCount, Integer totalCount) {
    this.score = score;
    this.passed = passed;
    this.correctCount = correctCount;
    this.totalCount = totalCount;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public Integer getCorrectCount() {
    return correctCount;
  }

  public void setCorrectCount(Integer correctCount) {
    this.correctCount = correctCount;
  }

  public Integer getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }
}
