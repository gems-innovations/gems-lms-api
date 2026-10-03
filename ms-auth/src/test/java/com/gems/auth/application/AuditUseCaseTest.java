package com.gems.auth.application;

import com.gems.auth.application.gateway.AuditGateway;
import com.gems.auth.domain.entities.AuditEvent;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditUseCaseTest {
  private final AuditGateway gateway = mock(AuditGateway.class);
  private final AuditUseCase useCase = new AuditUseCase(gateway);

  @Test
  void recordsTheActorAndBoundsRequestMetadata() {
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    StepVerifier.create(useCase.record(7L, "ADMIN", "inst-1", "UPDATE", "PUT", "/api/v1/courses/4",
      200, "1".repeat(120), "a".repeat(550)))
      .assertNext(event -> {
        assertEquals(7L, event.actorUserId());
        assertEquals(100, event.clientIp().length());
        assertEquals(500, event.userAgent().length());
      }).verifyComplete();
  }
}
