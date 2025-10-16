package com.gems.auth.infrastructure.driving.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response information")
public class ErrorResponse {
    @Schema(description = "Error code", example = "USER_ALREADY_EXISTS")
    private final String code;
    
    @Schema(description = "Error message", example = "User with email john.doe@example.com already exists")
    private final String message;
    
    @Schema(description = "HTTP status code", example = "400")
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