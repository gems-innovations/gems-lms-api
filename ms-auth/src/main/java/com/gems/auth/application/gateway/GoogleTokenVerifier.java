package com.gems.auth.application.gateway;

import reactor.core.publisher.Mono;

/** Checks a Google Identity Services ID token and tells who signed in. */
public interface GoogleTokenVerifier {
  /** False when no Google client id is configured: the feature is switched off. */
  boolean isEnabled();

  /**
   * Verifies signature, issuer, audience, expiry and that Google verified the e-mail.
   * Fails with InvalidCredentialsException otherwise. Implementations must never log the token.
   */
  Mono<GoogleIdentity> verify(String credential);

  /** The verified claims we use. Every field except {@code email} may be null. */
  record GoogleIdentity(String email, String name, String givenName, String familyName, String picture) {}
}
