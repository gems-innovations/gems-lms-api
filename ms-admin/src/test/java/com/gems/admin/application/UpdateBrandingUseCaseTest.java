package com.gems.admin.application;

import com.gems.admin.application.command.BrandingCommand;
import com.gems.admin.application.exceptions.BrandingNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateBrandingUseCaseTest {

  @Mock
  private BrandingGateway brandingGateway;

  @InjectMocks
  private UpdateBrandingUseCase updateBrandingUseCase;

  private BrandingCommand updateCommand;
  private Branding existingBranding;
  private Branding updatedBranding;

  @BeforeEach
  void setUp() {
    updateCommand = new BrandingCommand(
      "company-123",
      "updated.com",
      "https://cdn.example.com/new-logo.png",
      "https://cdn.example.com/new-favicon.ico",
      "#FF0000",
      "#00FF00",
      "#0000FF",
      "#FFFFFF",
      "dark",
      "https://cdn.example.com/new-bg.jpg",
      ".custom { color: blue; }"
    );

    LocalDateTime now = LocalDateTime.now();
    existingBranding = new Branding(
      1L, "company-123", "example.com",
      "https://cdn.example.com/logo.png",
      "https://cdn.example.com/favicon.ico",
      "#3B82F6", "#8B5CF6", "#10B981", "#1F2937",
      "light", "https://cdn.example.com/bg.jpg",
      ".custom { color: red; }", now
    );

    updatedBranding = new Branding(
      1L, "company-123", "updated.com",
      "https://cdn.example.com/new-logo.png",
      "https://cdn.example.com/new-favicon.ico",
      "#FF0000", "#00FF00", "#0000FF", "#FFFFFF",
      "dark", "https://cdn.example.com/new-bg.jpg",
      ".custom { color: blue; }", LocalDateTime.now()
    );
  }

  @Test
  void shouldUpdateBrandingSuccessfully() {
    // Given
    when(brandingGateway.findByCompanyId("company-123")).thenReturn(Mono.just(existingBranding));
    when(brandingGateway.update(any(Branding.class))).thenReturn(Mono.just(updatedBranding));

    // When
    Mono<BrandingResponse> result = updateBrandingUseCase.execute("company-123", updateCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.domain().equals("updated.com") &&
          response.primaryColor().equals("#FF0000") &&
          response.theme().equals("dark")
      )
      .verifyComplete();

    verify(brandingGateway, times(1)).findByCompanyId("company-123");
    verify(brandingGateway, times(1)).update(any(Branding.class));
  }

  @Test
  void shouldThrowExceptionWhenBrandingNotFound() {
    // Given
    when(brandingGateway.findByCompanyId(anyString())).thenReturn(Mono.empty());

    // When
    Mono<BrandingResponse> result = updateBrandingUseCase.execute("nonexistent", updateCommand);

    // Then
    StepVerifier.create(result)
      .expectError(BrandingNotFoundException.class)
      .verify();

    verify(brandingGateway, times(1)).findByCompanyId("nonexistent");
    verify(brandingGateway, never()).update(any(Branding.class));
  }
}
