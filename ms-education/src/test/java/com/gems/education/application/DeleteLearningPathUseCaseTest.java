package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.domain.entities.LearningPath;
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
class DeleteLearningPathUseCaseTest {

  @Mock
  private LearningPathGateway learningPathGateway;

  @InjectMocks
  private DeleteLearningPathUseCase deleteLearningPathUseCase;

  @Test
  void shouldDeleteLearningPathSuccessfully() {
    LearningPath lp = new LearningPath(1L, "LP 1", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(learningPathGateway.findById(1L)).thenReturn(Mono.just(lp));
    when(learningPathGateway.deleteById(1L)).thenReturn(Mono.empty());

    Mono<Void> result = deleteLearningPathUseCase.execute(1L);

    StepVerifier.create(result)
      .verifyComplete();

    verify(learningPathGateway, times(1)).findById(1L);
    verify(learningPathGateway, times(1)).deleteById(1L);
  }
}
