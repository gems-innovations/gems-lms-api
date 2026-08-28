package com.gems.admin.infrastructure.driving.rest;

import com.gems.admin.application.CreateBrandingUseCase;
import com.gems.admin.application.DeleteBrandingUseCase;
import com.gems.admin.application.GetBrandingByCompanyIdUseCase;
import com.gems.admin.application.UpdateBrandingUseCase;
import com.gems.admin.application.command.BrandingCommand;
import com.gems.admin.application.response.BrandingResponse;
import com.gems.admin.infrastructure.driving.rest.request.BrandingRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BrandingControllerTest {

  private CreateBrandingUseCase createBrandingUseCase;
  private UpdateBrandingUseCase updateBrandingUseCase;
  private GetBrandingByCompanyIdUseCase getBrandingByCompanyIdUseCase;
  private DeleteBrandingUseCase deleteBrandingUseCase;
  private WebTestClient webTestClient;

  private BrandingResponse brandingResponse;

  @BeforeEach
  void setUp() {
    createBrandingUseCase = Mockito.mock(CreateBrandingUseCase.class);
    updateBrandingUseCase = Mockito.mock(UpdateBrandingUseCase.class);
    getBrandingByCompanyIdUseCase = Mockito.mock(GetBrandingByCompanyIdUseCase.class);
    deleteBrandingUseCase = Mockito.mock(DeleteBrandingUseCase.class);

    BrandingController brandingController = new BrandingController(
      createBrandingUseCase,
      updateBrandingUseCase,
      getBrandingByCompanyIdUseCase,
      deleteBrandingUseCase
    );
    webTestClient = WebTestClient.bindToController(brandingController).build();

    LocalDateTime now = LocalDateTime.now();
    brandingResponse = new BrandingResponse(
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
    BrandingRequest request = new BrandingRequest();
    request.setCompanyId("company-123");
    request.setDomain("example.com");
    request.setPrimaryColor("#3B82F6");
    request.setTheme("light");

    when(createBrandingUseCase.execute(any(BrandingCommand.class))).thenReturn(Mono.just(brandingResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/branding")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(BrandingResponse.class)
      .value(response -> {
        Assertions.assertEquals(1L, response.brandingId());
        Assertions.assertEquals("company-123", response.companyId());
        Assertions.assertEquals("example.com", response.domain());
      });

    verify(createBrandingUseCase, times(1)).execute(any(BrandingCommand.class));
  }

  @Test
  void shouldGetBrandingByCompanyIdSuccessfully() {
    // Given
    when(getBrandingByCompanyIdUseCase.execute("company-123")).thenReturn(Mono.just(brandingResponse));

    // When & Then
    webTestClient
      .get()
      .uri("/api/v1/branding/company-123")
      .exchange()
      .expectStatus().isOk()
      .expectBody(BrandingResponse.class)
      .value(response -> {
        Assertions.assertEquals("company-123", response.companyId());
        Assertions.assertEquals("example.com", response.domain());
      });

    verify(getBrandingByCompanyIdUseCase, times(1)).execute("company-123");
  }

  @Test
  void shouldUpdateBrandingSuccessfully() {
    // Given
    BrandingRequest updateRequest = new BrandingRequest();
    updateRequest.setCompanyId("company-123");
    updateRequest.setDomain("updated.com");
    updateRequest.setPrimaryColor("#FF0000");
    updateRequest.setTheme("dark");

    BrandingResponse updatedResponse = new BrandingResponse(
      1L, "company-123", "updated.com",
      "https://cdn.example.com/logo.png",
      "https://cdn.example.com/favicon.ico",
      "#FF0000", "#8B5CF6", "#10B981", "#1F2937",
      "dark", "https://cdn.example.com/bg.jpg",
      ".custom { color: red; }", LocalDateTime.now()
    );

    when(updateBrandingUseCase.execute(anyString(), any(BrandingCommand.class)))
      .thenReturn(Mono.just(updatedResponse));

    // When & Then
    webTestClient
      .put()
      .uri("/api/v1/branding/company-123")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus().isOk()
      .expectBody(BrandingResponse.class)
      .value(response -> {
        Assertions.assertEquals("updated.com", response.domain());
        Assertions.assertEquals("#FF0000", response.primaryColor());
        Assertions.assertEquals("dark", response.theme());
      });

    verify(updateBrandingUseCase, times(1)).execute(anyString(), any(BrandingCommand.class));
  }

  @Test
  void shouldDeleteBrandingSuccessfully() {
    // Given
    when(deleteBrandingUseCase.execute("company-123")).thenReturn(Mono.empty());

    // When & Then
    webTestClient
      .delete()
      .uri("/api/v1/branding/company-123")
      .exchange()
      .expectStatus().isNoContent();

    verify(deleteBrandingUseCase, times(1)).execute("company-123");
  }

  @Test
  void shouldReturnCreatedStatusOnCreate() {
    // Given
    BrandingRequest request = new BrandingRequest();
    request.setCompanyId("company-456");

    when(createBrandingUseCase.execute(any(BrandingCommand.class))).thenReturn(Mono.just(brandingResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/branding")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated();

    verify(createBrandingUseCase, times(1)).execute(any(BrandingCommand.class));
  }
}
