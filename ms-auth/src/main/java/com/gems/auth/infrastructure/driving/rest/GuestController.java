package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.EmailPreferences;
import com.gems.auth.application.EmailPreferencesUseCase;
import com.gems.auth.application.EmailVerificationUseCase;
import com.gems.auth.application.gateway.ConsentGateway;
import com.gems.auth.application.GuestAccessUseCase;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.RateLimitFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Free access without registering: start as a guest, claim the account later. */
@RestController
@RequestMapping(AuthInfraConstants.AUTH_API_BASE_PATH + "/guest")
@Tag(name = "Authentication")
public class GuestController {
  private final GuestAccessUseCase guests;
  private final EmailVerificationUseCase verification;
  private final EmailPreferencesUseCase preferences;
  private final ConsentGateway consents;
  private final String policyVersion;
  private final int maxPerHour;
  /** Guests created per client address in the current hour (each instance keeps its own count). */
  private final Map<String, int[]> created = new ConcurrentHashMap<>();

  public GuestController(GuestAccessUseCase guests, EmailVerificationUseCase verification,
                         EmailPreferencesUseCase preferences, ConsentGateway consents,
                         @Value("${app.legal.policy-version:2026-10}") String policyVersion,
                         @Value("${guest.max-per-hour:30}") int maxPerHour) {
    this.verification = verification;
    this.preferences = preferences;
    this.consents = consents;
    this.policyVersion = policyVersion;
    this.guests = guests;
    this.maxPerHour = maxPerHour;
  }

  public record StartRequest(String nickname) {}

  /**
   * @param acceptDataPolicy required: the user authorizes the processing of their data (Ley 1581 de 2012)
   * @param acceptTips       optional, unchecked by default: reminders and ideas by e-mail
   */
  public record ClaimRequest(String firstName, String lastName, String email, String password,
                             Boolean acceptDataPolicy, Boolean acceptTips) {}

  @PostMapping
  @Operation(summary = "Start as a guest", description = "Creates a guest student in the open institution and signs them in.")
  public Mono<ResponseEntity<LoginResponse>> start(@RequestBody(required = false) StartRequest body, ServerWebExchange exchange) {
    String client = RateLimitFilter.getClientId(exchange.getRequest());
    int hour = (int) (Instant.now().getEpochSecond() / 3600);
    int[] slot = created.compute(client, (k, v) -> v == null || v[0] != hour ? new int[]{hour, 1} : new int[]{hour, v[1] + 1});
    if (slot[1] > maxPerHour) {
      return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many guest sessions from this network. Try again later"));
    }
    if (created.size() > 50_000) created.entrySet().removeIf(e -> e.getValue()[0] != hour);
    return guests.start(body == null ? null : body.nickname())
      .map(session -> ResponseEntity.status(HttpStatus.CREATED).body(session));
  }

  /** The signed-in guest becomes a regular student with their own email and password. */
  @PostMapping("/claim")
  @Operation(summary = "Claim a guest account", description = "Keeps all progress and returns a new session.")
  public Mono<ResponseEntity<LoginResponse>> claim(@RequestBody ClaimRequest body) {
    if (body == null || isBlank(body.firstName()) || isBlank(body.lastName()) || isBlank(body.email()) || isBlank(body.password())) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, last name, email and password are required"));
    }
    if (!Boolean.TRUE.equals(body.acceptDataPolicy())) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must accept the data processing policy"));
    }
    boolean tips = Boolean.TRUE.equals(body.acceptTips());
    return CurrentUser.get()
      .flatMap(caller -> guests.claim(caller.userId(), body.firstName(), body.lastName(), body.email(), body.password())
        // Proof of the authorization, and tips only if the user ticked them.
        .flatMap(session -> consents.record(caller.userId(), ConsentGateway.DATA_POLICY, policyVersion, true)
          .then(consents.record(caller.userId(), ConsentGateway.TIPS, policyVersion, tips))
          .then(preferences.update(caller.userId(), new EmailPreferences(true, tips)))
          .thenReturn(session))
        .doOnSuccess(session -> verification.send(caller.userId()).onErrorResume(e -> Mono.empty()).subscribe()))
      .map(ResponseEntity::ok);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
