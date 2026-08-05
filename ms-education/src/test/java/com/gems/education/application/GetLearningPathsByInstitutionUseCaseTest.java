package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.LearningPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetLearningPathsByInstitutionUseCaseTest {

  @Mock
  private LearningPathGateway learningPathGateway;

  @InjectMocks
  private GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase;

  @Test
  void shouldReturnLearningPathsForInstitution() {
    LearningPath lp = new LearningPath(1L, "LP 1", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(learningPathGateway.findByInstitutionId("inst-1")).thenReturn(Flux.just(lp));

    Flux<LearningPathResponse> result = getLearningPathsByInstitutionUseCase.execute("inst-1");

    StepVerifier.create(result)
      .expectNextMatches(res -> res.getId().equals(1L) && res.getInstitutionId().equals("inst-1"))
      .verifyComplete();

    verify(learningPathGateway, times(1)).findByInstitutionId("inst-1");
  }
}
