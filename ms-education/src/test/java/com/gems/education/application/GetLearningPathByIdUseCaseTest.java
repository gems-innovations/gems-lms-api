package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.infrastructure.driving.rest.exeption.LearningPathNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetLearningPathByIdUseCaseTest {

  @Mock
  private LearningPathGateway learningPathGateway;

  @InjectMocks
  private GetLearningPathByIdUseCase getLearningPathByIdUseCase;

  @Test
  void shouldReturnLearningPathSuccessfully() {
    LearningPath lp = new LearningPath(1L, "LP 1", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(learningPathGateway.findById(1L)).thenReturn(Mono.just(lp));

    Mono<LearningPathResponse> result = getLearningPathByIdUseCase.execute(1L);

    StepVerifier.create(result)
      .expectNextMatches(res -> res.getId().equals(1L) && res.getTitle().equals("LP 1"))
      .verifyComplete();

    verify(learningPathGateway, times(1)).findById(1L);
  }

  @Test
  void shouldThrowNotFoundWhenLearningPathDoesNotExist() {
    when(learningPathGateway.findById(2L)).thenReturn(Mono.empty());

    Mono<LearningPathResponse> result = getLearningPathByIdUseCase.execute(2L);

    StepVerifier.create(result)
      .expectError(LearningPathNotFoundException.class)
      .verify();

    verify(learningPathGateway, times(1)).findById(2L);
  }
}
