package com.gems.admin.infrastructure.driving.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response object containing error details")
public class ErrorResponse {
  @Schema(description = "Error code identifying the type of error", example = "BRANDING_NOT_FOUND")
  private final String code;

  @Schema(description = "Human-readable error message", example = "Branding not found for company")
  private final String message;

  @Schema(description = "HTTP status code", example = "404")
  private final int status;

  public ErrorResponse(String code, String message, int status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public int getStatus() {
    return status;
  }
}
