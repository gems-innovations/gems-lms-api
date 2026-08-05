package com.gems.admin.application;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.command.InstitutionMetadataCommand;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateInstitutionUseCaseTest {

  @Mock
  private InstitutionGateway institutionGateway;

  @InjectMocks
  private UpdateInstitutionUseCase updateInstitutionUseCase;

  private InstitutionCommand updateCommand;
  private Institution existingInstitution;
  private Institution updatedInstitution;

  @BeforeEach
  void setUp() {
    InstitutionMetadataCommand metadataCommand = new InstitutionMetadataCommand(
      "Gems College Updated Description",
      "https://gems-new.edu",
      "contact-new@gems.edu",
      "987654321",
      "456 New Street",
      "ENTERPRISE",
      1000
    );

    updateCommand = new InstitutionCommand(
      "inst-1",
      "Gems College New",
      "UNIVERSITY",
      "ACTIVE",
      metadataCommand
    );

    InstitutionMetadata existingMetadata = new InstitutionMetadata(
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

    existingInstitution = new Institution(
      "inst-1",
      "Gems College",
      "COLLEGE",
      "ACTIVE",
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      existingMetadata
    );

    InstitutionMetadata updatedMetadata = new InstitutionMetadata(
      "inst-1",
      "Gems College Updated Description",
      "https://gems-new.edu",
      "contact-new@gems.edu",
      "987654321",
      "456 New Street",
      "ENTERPRISE",
      1000,
      LocalDateTime.now()
    );

    updatedInstitution = new Institution(
      "inst-1",
      "Gems College New",
      "UNIVERSITY",
      "ACTIVE",
      0,
      existingInstitution.getCreatedAt(),
      LocalDateTime.now(),
      updatedMetadata
    );
  }

  @Test
  void shouldUpdateInstitutionSuccessfully() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.just(existingInstitution));
    when(institutionGateway.update(any(Institution.class))).thenReturn(Mono.just(updatedInstitution));

    // When
    Mono<InstitutionResponse> result = updateInstitutionUseCase.execute("inst-1", updateCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.id().equals("inst-1") &&
          response.name().equals("Gems College New") &&
          response.type().equals("UNIVERSITY") &&
          response.metadata().maxUsers() == 1000 &&
          response.metadata().subscriptionType().equals("ENTERPRISE")
      )
      .verifyComplete();

    verify(institutionGateway, times(1)).findById("inst-1");
    verify(institutionGateway, times(1)).update(any(Institution.class));
  }

  @Test
  void shouldThrowExceptionWhenUpdatingNonExistentInstitution() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.empty());

    // When
    Mono<InstitutionResponse> result = updateInstitutionUseCase.execute("inst-1", updateCommand);

    // Then
    StepVerifier.create(result)
      .expectError(InstitutionNotFoundException.class)
      .verify();

    verify(institutionGateway, times(1)).findById("inst-1");
    verify(institutionGateway, never()).update(any(Institution.class));
  }
}
