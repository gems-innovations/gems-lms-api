package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.GoogleSignInUseCase;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.JwtServerAuthenticationConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Sign in or sign up with a Google ID token. Public; answers 404 when Google sign-in is not configured. */
@RestController
@RequestMapping(AuthInfraConstants.AUTH_API_BASE_PATH + "/google")
@Tag(name = "Authentication")
public class GoogleAuthController {
  private final GoogleSignInUseCase googleSignIn;
  private final ReactiveAuthenticationManager sessions;
  private final JwtServerAuthenticationConverter bearer = new JwtServerAuthenticationConverter();

  public GoogleAuthController(GoogleSignInUseCase googleSignIn, ReactiveAuthenticationManager sessions) {
    this.googleSignIn = googleSignIn;
    this.sessions = sessions;
  }

  /**
   * @param credential   the ID token (JWT) from Google Identity Services
   * @param acceptPolicy required to create an account; not needed to sign in to an existing one
   * @param tips         optional, off by default: reminders and ideas by e-mail
   */
  public record GoogleSignInRequest(String credential, Boolean acceptPolicy, Boolean tips) {}

  @PostMapping
  @Operation(summary = "Sign in or sign up with Google",
    description = "Verifies the Google ID token. Signs in the account with that e-mail, converts the guest session "
      + "sent as Bearer token, or creates a student in the open institution.")
  public Mono<ResponseEntity<LoginResponse>> signIn(@RequestBody(required = false) GoogleSignInRequest body,
                                                    ServerWebExchange exchange) {
    if (!googleSignIn.isEnabled()) {
      return Mono.error(new com.gems.auth.application.exceptions.GoogleSignInDisabledException());
    }
    String credential = body == null ? null : body.credential();
    boolean accept = body != null && Boolean.TRUE.equals(body.acceptPolicy());
    boolean tips = body != null && Boolean.TRUE.equals(body.tips());
    return guestSession(exchange)
      .map(java.util.Optional::of).defaultIfEmpty(java.util.Optional.empty())
      .flatMap(id -> googleSignIn.execute(credential, accept, tips, id.orElse(null)))
      .map(ResponseEntity::ok);
  }

  /** The user behind the optional Bearer token; empty if there is none or it is not valid. */
  private Mono<Long> guestSession(ServerWebExchange exchange) {
    return bearer.convert(exchange)
      .flatMap(sessions::authenticate)
      .mapNotNull(auth -> auth.getPrincipal() instanceof AuthenticatedUser user ? user.userId() : null)
      .onErrorResume(e -> Mono.empty());
  }
}
