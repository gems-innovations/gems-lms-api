package com.gems.admin.application;

import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Institution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAllInstitutionsUseCaseTest {

  @Mock
  private InstitutionGateway institutionGateway;

  @InjectMocks
  private GetAllInstitutionsUseCase getAllInstitutionsUseCase;

  private Institution institution1;
  private Institution institution2;

  @BeforeEach
  void setUp() {
    institution1 = new Institution(
      "inst-1",
      "Gems College",
      "COLLEGE",
      "ACTIVE",
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      null
    );

    institution2 = new Institution(
      "inst-2",
      "Gems School",
      "SCHOOL",
      "ACTIVE",
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      null
    );
  }

  @Test
  void shouldReturnAllInstitutions() {
    // Given
    when(institutionGateway.findAll()).thenReturn(Flux.just(institution1, institution2));

    // When
    Flux<InstitutionResponse> result = getAllInstitutionsUseCase.execute();

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response -> response.id().equals("inst-1") && response.name().equals("Gems College"))
      .expectNextMatches(response -> response.id().equals("inst-2") && response.name().equals("Gems School"))
      .verifyComplete();

    verify(institutionGateway, times(1)).findAll();
  }
}
