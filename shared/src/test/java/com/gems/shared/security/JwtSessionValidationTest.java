package com.gems.shared.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtSessionValidationTest {
  private static final String SECRET = "k".repeat(64);

  private String token(String revision) {
    return Jwts.builder().setSubject("5").claim("role", "STUDENT").claim("institutionId", "inst-1")
      .claim("sessionRevision", revision).setExpiration(new Date(System.currentTimeMillis() + 60000))
      .signWith(Keys.hmacShaKeyFor(SECRET.getBytes())).compact();
  }

  private JwtReactiveAuthenticationManager manager(SessionValidator validator) throws Exception {
    var manager = new JwtReactiveAuthenticationManager(validator);
    var field = JwtReactiveAuthenticationManager.class.getDeclaredField("jwtSecret");
    field.setAccessible(true);
    field.set(manager, SECRET);
    return manager;
  }

  @Test
  void rejectsDisabledDeletedAndChangedAccounts() throws Exception {
    var request = new UsernamePasswordAuthenticationToken(null, token("v1"));
    for (SessionValidator.State state : new SessionValidator.State[] {
      new SessionValidator.State("v1", "STUDENT", "inst-1", false, false),
      new SessionValidator.State("v2", "STUDENT", "inst-1", true, false),
      new SessionValidator.State("v1", "ADMIN", "inst-1", true, false),
      new SessionValidator.State("v1", "STUDENT", "inst-2", true, false)
    }) {
      var manager = manager((id, jwt) -> Mono.just(state));
      assertThrows(BadCredentialsException.class, () -> manager.authenticate(request).block());
    }
    var deleted = manager((id, jwt) -> Mono.empty());
    assertThrows(BadCredentialsException.class, () -> deleted.authenticate(request).block());
  }

  @Test
  void acceptsCurrentAccountAndReadsTemporaryPasswordFlagFromCurrentState() throws Exception {
    var manager = manager((id, jwt) -> Mono.just(new SessionValidator.State("v1", "STUDENT", "inst-1", true, true)));
    var auth = manager.authenticate(new UsernamePasswordAuthenticationToken(null, token("v1"))).block();
    var caller = (AuthenticatedUser) auth.getPrincipal();
    assertTrue(caller.mustChangePassword());
    assertThrows(ForbiddenException.class, () -> CurrentUser.get()
      .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)).block());
    assertEquals(caller, CurrentUser.forPasswordChange()
      .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)).block());
  }

  @Test
  void rejectsTokensCreatedBeforeSessionValidationWasAdded() throws Exception {
    var manager = manager((id, jwt) -> Mono.error(new AssertionError("Legacy token must fail before lookup")));
    assertThrows(BadCredentialsException.class, () -> manager.authenticate(
      new UsernamePasswordAuthenticationToken(null, token(null))).block());
  }
}
