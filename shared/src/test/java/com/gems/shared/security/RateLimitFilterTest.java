package com.gems.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.net.InetSocketAddress;

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
      .signWith(JwtReactiveAuthenticationManager.signingKey(SECRET)).compact();
    var signed = MockServerHttpRequest.get("/api/v1/courses").header("Authorization", "Bearer " + token).build();
    assertEquals("42", RateLimitFilter.signedSubject(signed, SECRET));

    String forged = io.jsonwebtoken.Jwts.builder().setSubject("42")
      .signWith(JwtReactiveAuthenticationManager.signingKey(SECRET + "x")).compact();
    var other = MockServerHttpRequest.get("/api/v1/courses").header("Authorization", "Bearer " + forged).build();
    assertNull(RateLimitFilter.signedSubject(other, SECRET));
  }
}
