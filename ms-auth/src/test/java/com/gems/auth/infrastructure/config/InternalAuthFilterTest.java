package com.gems.auth.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("InternalAuthFilter Tests")
class InternalAuthFilterTest {

  private InternalAuthFilter filter;

  private final String headerName = "X-Internal-Token";
  private final String secret = "super-secret";

  @BeforeEach
  void setUp() {
    filter = new InternalAuthFilter();
    ReflectionTestUtils.setField(filter, "internalHeader", headerName);
    ReflectionTestUtils.setField(filter, "internalSecret", secret);
  }

  @Nested
  @DisplayName("Filter behavior")
  class FilterBehavior {

    @Test
    @DisplayName("Should allow when token matches secret")
    void shouldAllowWhenTokenMatchesSecret() {
      MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
          .header(headerName, secret)
          .build();
      MockServerWebExchange exchange = MockServerWebExchange.from(request);

      WebFilterChain chain = mock(WebFilterChain.class);
      when(chain.filter(exchange)).thenReturn(Mono.empty());

      Mono<Void> result = filter.filter(exchange, chain);

      StepVerifier.create(result).verifyComplete();
      verify(chain).filter(exchange);
      assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Should forbid when token missing")
    void shouldForbidWhenTokenMissing() {
      MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
      MockServerWebExchange exchange = MockServerWebExchange.from(request);

      WebFilterChain chain = mock(WebFilterChain.class);

      Mono<Void> result = filter.filter(exchange, chain);

      StepVerifier.create(result).verifyComplete();
      assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
      verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("Should forbid when token is wrong")
    void shouldForbidWhenTokenIsWrong() {
      MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
          .header(headerName, "wrong-token")
          .build();
      MockServerWebExchange exchange = MockServerWebExchange.from(request);

      WebFilterChain chain = mock(WebFilterChain.class);

      Mono<Void> result = filter.filter(exchange, chain);

      StepVerifier.create(result).verifyComplete();
      assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
      verifyNoInteractions(chain);
    }
  }
}
