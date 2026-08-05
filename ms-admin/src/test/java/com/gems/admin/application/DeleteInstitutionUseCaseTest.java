package com.gems.admin.application;

import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.domain.entities.Institution;
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
class DeleteInstitutionUseCaseTest {

  @Mock
  private InstitutionGateway institutionGateway;

  @InjectMocks
  private DeleteInstitutionUseCase deleteInstitutionUseCase;

  private Institution institution;

  @BeforeEach
  void setUp() {
    institution = new Institution(
      "inst-1",
      "Gems College",
      "COLLEGE",
      "ACTIVE",
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      null
    );
  }

  @Test
  void shouldDeleteInstitutionSuccessfully() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.just(institution));
    when(institutionGateway.deleteById("inst-1")).thenReturn(Mono.empty());

    // When
    Mono<Void> result = deleteInstitutionUseCase.execute("inst-1");

    // Then
    StepVerifier.create(result)
      .verifyComplete();

    verify(institutionGateway, times(1)).findById("inst-1");
    verify(institutionGateway, times(1)).deleteById("inst-1");
  }

  @Test
  void shouldThrowExceptionWhenDeletingNonExistentInstitution() {
    // Given
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.empty());

    // When
    Mono<Void> result = deleteInstitutionUseCase.execute("inst-1");

    // Then
    StepVerifier.create(result)
      .expectError(InstitutionNotFoundException.class)
      .verify();

    verify(institutionGateway, times(1)).findById("inst-1");
    verify(institutionGateway, never()).deleteById(anyString());
  }
}
