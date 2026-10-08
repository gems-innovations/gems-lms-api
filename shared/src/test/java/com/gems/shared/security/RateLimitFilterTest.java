package com.gems.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RateLimitFilterTest {

  @Test
  void aClientOnTheInternetCannotChooseItsKeyWithForwardingHeaders() {
    var request = MockServerHttpRequest.get("/api/v1/courses")
      .remoteAddress(new InetSocketAddress("203.0.113.7", 50000))
      .header("X-Forwarded-For", "1.2.3.4")
      .header("X-Real-IP", "5.6.7.8")
      .build();

    assertEquals("203.0.113.7", RateLimitFilter.getClientId(request));
  }

  @Test
  void behindALocalProxyTheRealClientIsUsed() {
    var viaNginx = MockServerHttpRequest.get("/api/v1/courses")
      .remoteAddress(new InetSocketAddress("172.18.0.5", 50000))
      .header("X-Real-IP", "198.51.100.20")
      .build();
    assertEquals("198.51.100.20", RateLimitFilter.getClientId(viaNginx));

    // The last hop is the one the trusted proxy appended; earlier ones come from the client.
    var forwarded = MockServerHttpRequest.get("/api/v1/courses")
      .remoteAddress(new InetSocketAddress("127.0.0.1", 50000))
      .header("X-Forwarded-For", "6.6.6.6, 198.51.100.21")
      .build();
    assertEquals("198.51.100.21", RateLimitFilter.getClientId(forwarded));
  }

  @Test
  void withoutProxyHeadersTheDirectPeerIsUsed() {
    var request = MockServerHttpRequest.get("/")
      .remoteAddress(new InetSocketAddress("127.0.0.1", 50000))
      .build();
    assertEquals("127.0.0.1", RateLimitFilter.getClientId(request));
  }

  private static final String SECRET = "a-test-secret-long-enough-for-hmac-sha-512-signing-0123456789abcdef";

  @Test
  void signedRequestsAreCountedPerUserAndForgedOnesAreNot() {
    String token = io.jsonwebtoken.Jwts.builder().setSubject("42")
      .signWith(JwtKeys.signingKey(SECRET)).compact();
    var signed = MockServerHttpRequest.get("/api/v1/courses").header("Authorization", "Bearer " + token).build();
    assertEquals("42", RateLimitFilter.signedSubject(signed, SECRET));

    String forged = io.jsonwebtoken.Jwts.builder().setSubject("42")
      .signWith(JwtKeys.signingKey(SECRET + "x")).compact();
    var other = MockServerHttpRequest.get("/api/v1/courses").header("Authorization", "Bearer " + forged).build();
    assertNull(RateLimitFilter.signedSubject(other, SECRET));
  }

  // ---- the filter itself, with a fake Redis ----

  @SuppressWarnings("unchecked")
  private RateLimitFilter filterWith(Mono<Long> counter) {
    var redis = org.mockito.Mockito.mock(ReactiveStringRedisTemplate.class);
    var ops = org.mockito.Mockito.mock(ReactiveValueOperations.class);
    org.mockito.Mockito.when(redis.opsForValue()).thenReturn(ops);
    org.mockito.Mockito.when(ops.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(counter);
    org.mockito.Mockito.when(redis.expire(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
      .thenReturn(Mono.just(true));
    var filter = new RateLimitFilter(redis);
    ReflectionTestUtils.setField(filter, "maxRequests", 2);
    ReflectionTestUtils.setField(filter, "maxAnonymousRequests", 2);
    ReflectionTestUtils.setField(filter, "jwtSecret", "");
    ReflectionTestUtils.setField(filter, "windowSeconds", 60);
    ReflectionTestUtils.setField(filter, "keyPrefix", "rl:");
    return filter;
  }

  private static MockServerWebExchange exchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/courses")
      .remoteAddress(new InetSocketAddress("203.0.113.7", 50000)).build());
  }

  @Test
  void aRequestOverTheLimitGets429AndNeverReachesTheApplication() {
    var calls = new AtomicInteger();
    var exchange = exchange();

    StepVerifier.create(filterWith(Mono.just(3L)).filter(exchange, e -> { calls.incrementAndGet(); return Mono.empty(); }))
      .verifyComplete();

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    assertEquals(0, calls.get());
  }

  @Test
  void redisBeingDownLetsTheRequestThroughOnce() {
    var calls = new AtomicInteger();

    StepVerifier.create(filterWith(Mono.error(new IllegalStateException("redis down")))
        .filter(exchange(), e -> { calls.incrementAndGet(); return Mono.empty(); }))
      .verifyComplete();

    assertEquals(1, calls.get());
  }

  @Test
  void anApplicationErrorIsNotMistakenForARedisFailureAndDoesNotRunTheRequestAgain() {
    var calls = new AtomicInteger();

    StepVerifier.create(filterWith(Mono.just(1L)).filter(exchange(),
        e -> { calls.incrementAndGet(); return Mono.error(new IllegalStateException("boom")); }))
      .expectError(IllegalStateException.class).verify();

    assertEquals(1, calls.get());
  }
}
