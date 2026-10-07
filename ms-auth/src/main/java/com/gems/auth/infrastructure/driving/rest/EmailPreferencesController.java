package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.EmailPreferences;
import com.gems.auth.application.EmailPreferencesUseCase;
import com.gems.auth.application.EmailVerificationUseCase;
import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import com.gems.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(AuthInfraConstants.AUTH_API_BASE_PATH + "/email-preferences")
@Tag(name = "Auth")
public class EmailPreferencesController {
  private final EmailPreferencesUseCase preferences;
  private final EmailVerificationUseCase verification;

  public EmailPreferencesController(EmailPreferencesUseCase preferences, EmailVerificationUseCase verification) {
    this.preferences = preferences;
    this.verification = verification;
  }

  public record PreferencesBody(boolean courseNotices, boolean tips) {
  }

  public record PreferencesView(boolean courseNotices, boolean tips, boolean emailVerified) {
  }

  @GetMapping
  @Operation(summary = "The signed-in user's e-mail preferences and whether their address is verified")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<PreferencesView> mine() {
    return CurrentUser.get().flatMap(caller -> view(caller.userId()));
  }

  @PutMapping
  @Operation(summary = "Change the signed-in user's e-mail preferences")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<PreferencesView> update(@RequestBody PreferencesBody body) {
    return CurrentUser.get().flatMap(caller -> preferences.update(caller.userId(), toPreferences(body))
      .then(view(caller.userId())));
  }

  /** Public: the link at the bottom of every e-mail opens this without signing in. */
  @GetMapping("/by-link")
  @Operation(summary = "Read e-mail preferences with the signed link from an e-mail")
  public Mono<PreferencesBody> byLink(@RequestParam("t") String token) {
    return preferences.getWithToken(token)
      .map(p -> new PreferencesBody(p.courseNotices(), p.tips()))
      .switchIfEmpty(Mono.error(invalidLink()));
  }

  @PutMapping("/by-link")
  @Operation(summary = "Change e-mail preferences with the signed link from an e-mail")
  public Mono<PreferencesBody> updateByLink(@RequestParam("t") String token, @RequestBody PreferencesBody body) {
    return preferences.updateWithToken(token, toPreferences(body))
      .map(p -> new PreferencesBody(p.courseNotices(), p.tips()))
      .switchIfEmpty(Mono.error(invalidLink()));
  }

  private Mono<PreferencesView> view(Long userId) {
    return Mono.zip(preferences.get(userId), verification.isVerified(userId))
      .map(t -> new PreferencesView(t.getT1().courseNotices(), t.getT1().tips(), t.getT2()));
  }

  private static EmailPreferences toPreferences(PreferencesBody body) {
    if (body == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preferences are required");
    return new EmailPreferences(body.courseNotices(), body.tips());
  }

  private static ResponseStatusException invalidLink() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "The link is not valid");
  }
}
