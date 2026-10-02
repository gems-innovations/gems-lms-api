package com.gems.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

/** Access to the authenticated caller from reactive controllers. */
public final class CurrentUser {

  private CurrentUser() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** The caller, or a 403 when the request carries no authenticated user. */
  public static Mono<AuthenticatedUser> get() {
    return ReactiveSecurityContextHolder.getContext()
      .map(SecurityContext::getAuthentication)
      .map(Authentication::getPrincipal)
      .filter(AuthenticatedUser.class::isInstance)
      .cast(AuthenticatedUser.class)
      .switchIfEmpty(Mono.error(new ForbiddenException("Authentication required")));
  }

  /** The caller, if it satisfies {@code rule}; otherwise a 403 with {@code reason}. */
  public static Mono<AuthenticatedUser> require(Predicate<AuthenticatedUser> rule, String reason) {
    return get().flatMap(user -> rule.test(user) ? Mono.just(user) : Mono.error(new ForbiddenException(reason)));
  }
}
