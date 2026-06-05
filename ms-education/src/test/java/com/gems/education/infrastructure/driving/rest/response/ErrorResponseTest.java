package com.gems.education.infrastructure.driving.rest.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {

  @Test
  void shouldCreateErrorResponse() {
    ErrorResponse response = new ErrorResponse("ERR001", "Error message", 400);

    assertEquals("ERR001", response.getCode());
    assertEquals("Error message", response.getMessage());
    assertEquals(400, response.getStatus());
  }

  @Test
  void shouldStoreValuesCorrectly() {
    ErrorResponse response = new ErrorResponse("E100", "Invalid request", 422);

    assertNotNull(response);
    assertEquals("E100", response.getCode());
    assertEquals("Invalid request", response.getMessage());
    assertEquals(422, response.getStatus());
  }
}





