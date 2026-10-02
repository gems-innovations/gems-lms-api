package com.gems.auth.infrastructure.driving.rest;

import com.gems.shared.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilter;

import java.util.List;

/** Puts an authenticated caller in the reactive security context of controller tests. */
final class TestSecurity {

  private TestSecurity() {
  }

  static WebFilter authenticatedAs(AuthenticatedUser user) {
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
      user, "token", List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
    return (exchange, chain) -> chain.filter(exchange)
      .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
  }

  static WebFilter superAdmin() {
    return authenticatedAs(new AuthenticatedUser(9999L, AuthenticatedUser.SUPER_ADMIN, null));
  }
}
