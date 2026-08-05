package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.BulkEnrollmentCommand;
import com.gems.education.application.command.EnrollmentCommand;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.infrastructure.driving.rest.request.BulkEnrollmentRequest;
import com.gems.education.infrastructure.driving.rest.request.EnrollmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EnrollmentControllerTest {

  private EnrollStudentUseCase enrollStudentUseCase;
  private BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase;
  private GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase;
  private UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase;
  private EnrollmentController controller;

  @BeforeEach
  void setUp() {
    enrollStudentUseCase = mock(EnrollStudentUseCase.class);
    bulkEnrollStudentsUseCase = mock(BulkEnrollStudentsUseCase.class);
    getStudentEnrollmentsUseCase = mock(GetStudentEnrollmentsUseCase.class);
    updateEnrollmentProgressUseCase = mock(UpdateEnrollmentProgressUseCase.class);

    controller = new EnrollmentController(
      enrollStudentUseCase,
      bulkEnrollStudentsUseCase,
      getStudentEnrollmentsUseCase,
      updateEnrollmentProgressUseCase
    );
  }

  @Test
  void shouldEnrollStudent() {
    EnrollmentResponse response = new EnrollmentResponse(1L, 10L, 5L, LocalDateTime.now(), 0, null);
    when(enrollStudentUseCase.execute(any(EnrollmentCommand.class))).thenReturn(Mono.just(response));

    EnrollmentRequest request = new EnrollmentRequest(10L, 5L);

    StepVerifier.create(controller.enrollStudent(request))
      .assertNext(entity -> {
        assertEquals(201, entity.getStatusCode().value());
        assertEquals(10L, entity.getBody().getStudentId());
      })
      .verifyComplete();
  }

  @Test
  void shouldBulkEnroll() {
    EnrollmentResponse response = new EnrollmentResponse(1L, 10L, 5L, LocalDateTime.now(), 0, null);
    when(bulkEnrollStudentsUseCase.execute(any(BulkEnrollmentCommand.class))).thenReturn(Flux.just(response));

    BulkEnrollmentRequest request = new BulkEnrollmentRequest(List.of(10L), 5L);

    StepVerifier.create(controller.bulkEnrollStudents(request))
      .assertNext(entity -> {
        assertEquals(201, entity.getStatusCode().value());
        StepVerifier.create(entity.getBody())
          .assertNext(res -> assertEquals(10L, res.getStudentId()))
          .verifyComplete();
      })
      .verifyComplete();
  }

  @Test
  void shouldGetStudentEnrollments() {
    EnrollmentResponse response = new EnrollmentResponse(1L, 10L, 5L, LocalDateTime.now(), 0, null);
    when(getStudentEnrollmentsUseCase.execute(10L)).thenReturn(Flux.just(response));

    StepVerifier.create(controller.getStudentEnrollments(10L))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        StepVerifier.create(entity.getBody())
          .assertNext(res -> assertEquals(10L, res.getStudentId()))
          .verifyComplete();
      })
      .verifyComplete();
  }

  @Test
  void shouldUpdateProgress() {
    EnrollmentResponse response = new EnrollmentResponse(1L, 10L, 5L, LocalDateTime.now(), 40, null);
    when(updateEnrollmentProgressUseCase.execute(1L, 40)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.updateEnrollmentProgress(1L, 40))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals(40, entity.getBody().getProgress());
      })
      .verifyComplete();
  }
}
