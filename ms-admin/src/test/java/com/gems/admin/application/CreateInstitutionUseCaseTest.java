package com.gems.admin.application;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.command.InstitutionMetadataCommand;
import com.gems.admin.application.exceptions.InstitutionAlreadyExistsException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInstitutionUseCaseTest {

  @Mock
  private InstitutionGateway institutionGateway;

  @InjectMocks
  private CreateInstitutionUseCase createInstitutionUseCase;

  private InstitutionCommand validCommand;
  private Institution savedInstitution;

  @BeforeEach
  void setUp() {
    InstitutionMetadataCommand metadataCommand = new InstitutionMetadataCommand(
      "Gems College Description",
      "https://gems.edu",
      "contact@gems.edu",
      "123456789",
      "123 Street",
      "PREMIUM",
      500
    );

    validCommand = new InstitutionCommand(
      "inst-1",
      "Gems College",
      "COLLEGE",
      "ACTIVE",
      metadataCommand
    );

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

    savedInstitution = new Institution(
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
  void shouldCreateInstitutionSuccessfully() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.empty());
    when(institutionGateway.save(any(Institution.class))).thenReturn(Mono.just(savedInstitution));

    // When
    Mono<InstitutionResponse> result = createInstitutionUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.id().equals("inst-1") &&
          response.name().equals("Gems College") &&
          response.type().equals("COLLEGE") &&
          response.metadata() != null &&
          response.metadata().maxUsers() == 500
      )
      .verifyComplete();

    verify(institutionGateway, times(1)).findById("inst-1");
    verify(institutionGateway, times(1)).save(any(Institution.class));
  }

  @Test
  void shouldThrowExceptionWhenInstitutionAlreadyExists() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.just(savedInstitution));

    // When
    Mono<InstitutionResponse> result = createInstitutionUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectError(InstitutionAlreadyExistsException.class)
      .verify();

    verify(institutionGateway, times(1)).findById("inst-1");
    verify(institutionGateway, never()).save(any(Institution.class));
  }
}
