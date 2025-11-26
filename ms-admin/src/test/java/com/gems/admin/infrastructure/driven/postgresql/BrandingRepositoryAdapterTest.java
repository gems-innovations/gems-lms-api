package com.gems.admin.infrastructure.driven.postgresql;

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
class BrandingRepositoryAdapterTest {

  @Mock
  private IBrandingRepository brandingRepository;

  @InjectMocks
  private BrandingRepositoryAdapter brandingRepositoryAdapter;

  private Branding testBranding;
  private BrandingEntity testBrandingEntity;

  @BeforeEach
  void setUp() {
    LocalDateTime now = LocalDateTime.now();

    testBranding = new Branding(
      1L, "company-123", "example.com",
      "https://cdn.example.com/logo.png",
      "https://cdn.example.com/favicon.ico",
      "#3B82F6", "#8B5CF6", "#10B981", "#1F2937",
      "light", "https://cdn.example.com/bg.jpg",
      ".custom { color: red; }", now
    );

    testBrandingEntity = new BrandingEntity(
      1L, "company-123", "example.com",
      "https://cdn.example.com/logo.png",
      "https://cdn.example.com/favicon.ico",
      "#3B82F6", "#8B5CF6", "#10B981", "#1F2937",
      "light", "https://cdn.example.com/bg.jpg",
      ".custom { color: red; }", now
    );
  }

  @Test
  void shouldSaveBranding() {
    // Given
    when(brandingRepository.save(any(BrandingEntity.class))).thenReturn(Mono.just(testBrandingEntity));

    // When
    Mono<Branding> result = brandingRepositoryAdapter.save(testBranding);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(branding ->
        branding.getBrandingId().equals(1L) &&
          branding.getCompanyId().equals("company-123") &&
          branding.getDomain().equals("example.com")
      )
      .verifyComplete();

    verify(brandingRepository, times(1)).save(any(BrandingEntity.class));
  }

  @Test
  void shouldFindBrandingByCompanyId() {
    // Given
    when(brandingRepository.findByCompanyId("company-123")).thenReturn(Mono.just(testBrandingEntity));

    // When
    Mono<Branding> result = brandingRepositoryAdapter.findByCompanyId("company-123");

    // Then
    StepVerifier.create(result)
      .expectNextMatches(branding ->
        branding.getCompanyId().equals("company-123")
      )
      .verifyComplete();

    verify(brandingRepository, times(1)).findByCompanyId("company-123");
  }

  @Test
  void shouldUpdateBranding() {
    // Given
    when(brandingRepository.save(any(BrandingEntity.class))).thenReturn(Mono.just(testBrandingEntity));

    // When
    Mono<Branding> result = brandingRepositoryAdapter.update(testBranding);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(branding ->
        branding.getBrandingId().equals(1L)
      )
      .verifyComplete();

    verify(brandingRepository, times(1)).save(any(BrandingEntity.class));
  }

  @Test
  void shouldMapToDomainCorrectly() {
    // Given
    when(brandingRepository.findByCompanyId(anyString())).thenReturn(Mono.just(testBrandingEntity));

    // When
    Mono<Branding> result = brandingRepositoryAdapter.findByCompanyId("company-123");

    // Then
    StepVerifier.create(result)
      .expectNextMatches(branding ->
        branding.getBrandingId().equals(testBrandingEntity.getBrandingId()) &&
          branding.getCompanyId().equals(testBrandingEntity.getCompanyId()) &&
          branding.getPrimaryColor().equals(testBrandingEntity.getPrimaryColor())
      )
      .verifyComplete();
  }

  @Test
  void shouldMapToEntityCorrectly() {
    // Given
    when(brandingRepository.save(any(BrandingEntity.class))).thenAnswer(invocation -> {
      BrandingEntity entity = invocation.getArgument(0);
      return Mono.just(entity);
    });

    // When
    Mono<Branding> result = brandingRepositoryAdapter.save(testBranding);

    // Then
    StepVerifier.create(result)
      .expectNextCount(1)
      .verifyComplete();

    verify(brandingRepository).save(argThat(entity ->
      entity.getBrandingId().equals(testBranding.getBrandingId()) &&
        entity.getCompanyId().equals(testBranding.getCompanyId()) &&
        entity.getDomain().equals(testBranding.getDomain())
    ));
  }
}
