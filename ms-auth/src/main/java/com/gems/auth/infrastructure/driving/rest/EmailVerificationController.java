package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.EmailVerificationUseCase;
import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import com.gems.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping(AuthInfraConstants.AUTH_API_BASE_PATH + "/verify-email")
@Tag(name = "Auth")
public class EmailVerificationController {
  private final EmailVerificationUseCase verification;

  public EmailVerificationController(EmailVerificationUseCase verification) {
    this.verification = verification;
  }

  public record VerifyRequest(String token) {
  }

  /** Public: the link in the e-mail carries a one-time token. */
  @PostMapping
  @Operation(summary = "Confirm an e-mail address with the link token")
  public Mono<ResponseEntity<Void>> verify(@RequestBody VerifyRequest request) {
    return verification.confirm(request == null ? null : request.token())
      .flatMap(ok -> Boolean.TRUE.equals(ok)
        ? Mono.just(ResponseEntity.noContent().<Void>build())
        : Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "The verification link is invalid or has expired")));
  }

  /** The signed-in user asks for a new link. Always 202: sending is skipped quietly when not needed. */
  @PostMapping("/resend")
  @Operation(summary = "Send the verification link again")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<ResponseEntity<Void>> resend() {
    return CurrentUser.get()
      .flatMap(caller -> verification.send(caller.userId()))
      .thenReturn(ResponseEntity.accepted().<Void>build());
  }

  @GetMapping("/status")
  @Operation(summary = "Whether the signed-in user's e-mail is verified")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<Map<String, Boolean>> status() {
    return CurrentUser.get()
      .flatMap(caller -> verification.isVerified(caller.userId()))
      .map(verified -> Map.of("verified", verified));
  }
}
