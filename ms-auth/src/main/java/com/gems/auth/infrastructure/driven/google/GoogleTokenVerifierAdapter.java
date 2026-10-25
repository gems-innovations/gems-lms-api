package com.gems.auth.infrastructure.driven.google;

import com.gems.auth.application.exceptions.InvalidCredentialsException;
import com.gems.auth.application.gateway.GoogleTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/** Verifies Google ID tokens against Google's published signing keys (JWKS). Never logs the token. */
@Component
public class GoogleTokenVerifierAdapter implements GoogleTokenVerifier {
  private static final Logger LOG = LoggerFactory.getLogger(GoogleTokenVerifierAdapter.class);
  static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
  private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
  private static final String INVALID = "Invalid Google credential";

  private final String clientId;
  private final ReactiveJwtDecoder decoder;

  @org.springframework.beans.factory.annotation.Autowired
  public GoogleTokenVerifierAdapter(@Value("${app.google.client-id:}") String clientId) {
    this(clientId, clientId == null || clientId.isBlank() ? null : decoderFor(clientId.trim(), JWKS_URI));
  }

  /** For tests: a decoder that does not reach the network. */
  GoogleTokenVerifierAdapter(String clientId, ReactiveJwtDecoder decoder) {
    this.clientId = clientId == null ? "" : clientId.trim();
    this.decoder = decoder;
  }

  static ReactiveJwtDecoder decoderFor(String clientId, String jwksUri) {
    NimbusReactiveJwtDecoder nimbus = NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
    // The default validator checks exp / nbf; issuer, audience and verified e-mail are Google specific.
    nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), claims(clientId)));
    return nimbus;
  }

  static OAuth2TokenValidator<Jwt> claims(String clientId) {
    return jwt -> {
      String issuer = jwt.getClaimAsString("iss");
      List<String> audience = jwt.getAudience();
      Object verified = jwt.getClaim("email_verified");
      boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equals(verified);
      String email = jwt.getClaimAsString("email");
      if (issuer == null || !ISSUERS.contains(issuer)
          || audience == null || !audience.contains(clientId)
          || !emailVerified || email == null || email.isBlank() || !email.contains("@")) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", INVALID, null));
      }
      return OAuth2TokenValidatorResult.success();
    };
  }

  @Override
  public boolean isEnabled() {
    return !clientId.isBlank();
  }

  @Override
  public Mono<GoogleIdentity> verify(String credential) {
    if (!isEnabled() || decoder == null) return Mono.error(new InvalidCredentialsException(INVALID));
    return decoder.decode(credential)
      .map(jwt -> new GoogleIdentity(jwt.getClaimAsString("email"), jwt.getClaimAsString("name"),
        jwt.getClaimAsString("given_name"), jwt.getClaimAsString("family_name"), jwt.getClaimAsString("picture")))
      .onErrorMap(e -> {
        // Only the exception type: the message could echo parts of the token.
        LOG.warn("Google credential rejected ({})", e.getClass().getSimpleName());
        return new InvalidCredentialsException(INVALID);
      });
  }
}
