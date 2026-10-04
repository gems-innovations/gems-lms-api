package com.gems.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {
  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter();
    ReflectionTestUtils.setField(filter, "loginPath", "/api/v1/auth/login");
  }

  @Test
  void allowsHealthComponentsWithoutJwt() {
    assertPublic("/actuator/health/readiness");
  }

  @Test
  void allowsPrometheusScrapingWithoutJwt() {
    assertPublic("/actuator/prometheus");
  }

  @Test
  void stillRejectsProtectedRoutesWithoutJwt() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users").build());
    var called = new AtomicBoolean();

    StepVerifier.create(filter.filter(exchange, markingChain(called))).verifyComplete();

    assertFalse(called.get());
    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  private void assertPublic(String path) {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    var called = new AtomicBoolean();

    StepVerifier.create(filter.filter(exchange, markingChain(called))).verifyComplete();

    assertTrue(called.get());
  }

  private WebFilterChain markingChain(AtomicBoolean called) {
    return exchange -> {
      called.set(true);
      return Mono.empty();
    };
  }
}
