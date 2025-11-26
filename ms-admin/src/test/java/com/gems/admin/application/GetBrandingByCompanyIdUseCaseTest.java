package com.gems.admin.application;

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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetBrandingByCompanyIdUseCaseTest {

  @Mock
  private BrandingGateway brandingGateway;

  @InjectMocks
  private GetBrandingByCompanyIdUseCase getBrandingByCompanyIdUseCase;

  private Branding existingBranding;

  @BeforeEach
  void setUp() {
    LocalDateTime now = LocalDateTime.now();
    existingBranding = new Branding(
      1L, "company-123", "example.com",
      "https://cdn.example.com/logo.png",
      "https://cdn.example.com/favicon.ico",
      "#3B82F6", "#8B5CF6", "#10B981", "#1F2937",
      "light", "https://cdn.example.com/bg.jpg",
      ".custom { color: red; }", now
    );
  }

  @Test
  void shouldGetBrandingByCompanyIdSuccessfully() {
    // Given
    when(brandingGateway.findByCompanyId("company-123")).thenReturn(Mono.just(existingBranding));

    // When
    Mono<BrandingResponse> result = getBrandingByCompanyIdUseCase.execute("company-123");

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.brandingId().equals(1L) &&
          response.companyId().equals("company-123") &&
          response.domain().equals("example.com") &&
          response.primaryColor().equals("#3B82F6")
      )
      .verifyComplete();

    verify(brandingGateway, times(1)).findByCompanyId("company-123");
  }

  @Test
  void shouldThrowExceptionWhenBrandingNotFound() {
    // Given
    when(brandingGateway.findByCompanyId(anyString())).thenReturn(Mono.empty());

    // When
    Mono<BrandingResponse> result = getBrandingByCompanyIdUseCase.execute("nonexistent");

    // Then
    StepVerifier.create(result)
      .expectError(BrandingNotFoundException.class)
      .verify();

    verify(brandingGateway, times(1)).findByCompanyId("nonexistent");
  }
}
