package com.gems.education.application;

import com.gems.education.application.gateway.PathEnrollmentGateway;
import com.gems.education.domain.entities.PathEnrollment;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PathEnrollmentUseCaseTest {
  private final PathEnrollmentGateway gateway = mock(PathEnrollmentGateway.class);
  private final PathEnrollmentUseCase useCase = new PathEnrollmentUseCase(gateway);

  @Test
  void enrollingIsIdempotentAndSkipsDuplicates() {
    PathEnrollment existing = new PathEnrollment(1L, 7L, 5L, PathEnrollment.ACTIVE, LocalDateTime.now(), null);
    when(gateway.find(7L, 5L)).thenReturn(Mono.just(existing));
    when(gateway.find(7L, 6L)).thenReturn(Mono.empty());
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(useCase.enroll(7L, Arrays.asList(5L, 6L, 6L, null)))
      .assertNext(e -> assertEquals(1L, e.id()))
      .assertNext(e -> {
        assertEquals(6L, e.studentId());
        assertEquals(PathEnrollment.ACTIVE, e.status());
      })
      .verifyComplete();
    verify(gateway, times(1)).save(any());
  }
}
