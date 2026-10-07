package com.gems.admin.infrastructure.driving.rest;

import com.gems.shared.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilter;

import java.util.List;

/** Puts an authenticated caller in the reactive security context of controller tests. */
public final class TestSecurity {

  private TestSecurity() {
  }

  public static WebFilter authenticatedAs(AuthenticatedUser user) {
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
      user, "token", List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
    return (exchange, chain) -> chain.filter(exchange)
      .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
  }

  public static WebFilter superAdmin() {
    return authenticatedAs(new AuthenticatedUser(9999L, AuthenticatedUser.SUPER_ADMIN, null));
  }
}
