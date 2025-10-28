package com.gems.auth.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.reactive.CorsWebFilter;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CorsConfig Tests")
class CorsConfigTest {

  private CorsConfig corsConfig;

  @BeforeEach
  void setUp() {
    corsConfig = new CorsConfig();
    ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "https://example.com,https://another.com");
    ReflectionTestUtils.setField(corsConfig, "allowedMethods", "GET,POST");
    ReflectionTestUtils.setField(corsConfig, "allowedHeaders", "Content-Type,Authorization");
    ReflectionTestUtils.setField(corsConfig, "allowCredentials", true);
    ReflectionTestUtils.setField(corsConfig, "maxAge", 3600L);
  }

  @Test
  @DisplayName("Should create CorsWebFilter bean")
  void shouldCreateCorsWebFilterBean() {
    CorsWebFilter filter = corsConfig.corsWebFilter();
    assertNotNull(filter);
  }

  @Test
  @DisplayName("Should process preflight request without errors")
  void shouldProcessPreflightRequestWithoutErrors() {
    CorsWebFilter filter = corsConfig.corsWebFilter();

    MockServerHttpRequest request = MockServerHttpRequest.options("/api/test")
        .header(HttpHeaders.ORIGIN, "https://example.com")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
        .build();

    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(filter.filter(exchange, serverWebExchange -> exchange.getResponse().setComplete()))
        .verifyComplete();
  }
}
