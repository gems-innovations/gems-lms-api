package com.gems.admin.application;

import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Institution;
import com.gems.admin.domain.entities.InstitutionMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetInstitutionByIdUseCaseTest {

  @Mock
  private InstitutionGateway institutionGateway;

  @InjectMocks
  private GetInstitutionByIdUseCase getInstitutionByIdUseCase;

  private Institution institution;

  @BeforeEach
  void setUp() {
    InstitutionMetadata metadata = new InstitutionMetadata(
      "inst-1",
      "Gems College Description",
      "https://gems.edu",
      "contact@gems.edu",
      "123456789",
      "123 Street",
      "PREMIUM",
      500,
      LocalDateTime.now()
    );

    institution = new Institution(
      "inst-1",
      "Gems College",
      "COLLEGE",
      "ACTIVE",
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      metadata
    );
  }

  @Test
  void shouldReturnInstitutionSuccessfully() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.just(institution));

    // When
    Mono<InstitutionResponse> result = getInstitutionByIdUseCase.execute("inst-1");

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.id().equals("inst-1") &&
          response.name().equals("Gems College") &&
          response.metadata().contactEmail().equals("contact@gems.edu")
      )
      .verifyComplete();

    verify(institutionGateway, times(1)).findById("inst-1");
  }

  @Test
  void shouldThrowExceptionWhenInstitutionNotFound() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.empty());

    // When
    Mono<InstitutionResponse> result = getInstitutionByIdUseCase.execute("inst-1");

    // Then
    StepVerifier.create(result)
      .expectError(InstitutionNotFoundException.class)
      .verify();

    verify(institutionGateway, times(1)).findById("inst-1");
  }
}
