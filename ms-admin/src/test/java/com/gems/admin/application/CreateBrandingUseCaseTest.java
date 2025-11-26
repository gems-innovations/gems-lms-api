package com.gems.admin.application;

import com.gems.admin.application.command.BrandingCommand;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.response.BrandingResponse;
import com.gems.admin.domain.entities.Branding;
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
class CreateBrandingUseCaseTest {

  @Mock
  private BrandingGateway brandingGateway;

  @InjectMocks
  private CreateBrandingUseCase createBrandingUseCase;

  private BrandingCommand validCommand;
  private Branding savedBranding;

  @BeforeEach
  void setUp() {
    validCommand = new BrandingCommand(
      "company-123",
      "example.com",
      "https://cdn.example.com/logo.png",
      "https://cdn.example.com/favicon.ico",
      "#3B82F6",
      "#8B5CF6",
      "#10B981",
      "#1F2937",
      "light",
      "https://cdn.example.com/bg.jpg",
      ".custom { color: red; }"
    );

    LocalDateTime now = LocalDateTime.now();
    savedBranding = new Branding(
      1L, "company-123", "example.com",
      "https://cdn.example.com/logo.png",
      "https://cdn.example.com/favicon.ico",
      "#3B82F6", "#8B5CF6", "#10B981", "#1F2937",
      "light", "https://cdn.example.com/bg.jpg",
      ".custom { color: red; }", now
    );
  }

  @Test
  void shouldCreateBrandingSuccessfully() {
    // Given
    when(brandingGateway.save(any(Branding.class))).thenReturn(Mono.just(savedBranding));

    // When
    Mono<BrandingResponse> result = createBrandingUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.brandingId().equals(1L) &&
          response.companyId().equals("company-123") &&
          response.domain().equals("example.com") &&
          response.primaryColor().equals("#3B82F6") &&
          response.theme().equals("light")
      )
      .verifyComplete();

    verify(brandingGateway, times(1)).save(any(Branding.class));
  }

  @Test
  void shouldReturnBrandingResponseWithAllFields() {
    // Given
    when(brandingGateway.save(any(Branding.class))).thenReturn(Mono.just(savedBranding));

    // When
    Mono<BrandingResponse> result = createBrandingUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.brandingId() != null &&
          response.companyId() != null &&
          response.updatedAt() != null
      )
      .verifyComplete();
  }
}
