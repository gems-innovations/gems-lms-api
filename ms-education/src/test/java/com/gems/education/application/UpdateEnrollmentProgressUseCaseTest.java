package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Enrollment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateEnrollmentProgressUseCaseTest {

  @Mock
  private EnrollmentGateway enrollmentGateway;

  @InjectMocks
  private UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase;

  @Test
  void shouldUpdateProgressSuccessfully() {
    Enrollment enrollment = new Enrollment(1L, 10L, 5L, LocalDateTime.now(), 50, null);
    when(enrollmentGateway.findById(1L)).thenReturn(Mono.just(enrollment));
    when(enrollmentGateway.save(any(Enrollment.class))).thenReturn(Mono.just(enrollment));

    Mono<EnrollmentResponse> result = updateEnrollmentProgressUseCase.execute(1L, 100);

    StepVerifier.create(result)
      .assertNext(res -> {
        assertNotNull(res.getCompletedAt());
        verify(enrollmentGateway, times(1)).save(any(Enrollment.class));
      })
      .verifyComplete();
  }
}
