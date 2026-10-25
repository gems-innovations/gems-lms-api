package com.gems.auth.application;

import com.gems.auth.application.constants.AuthAppConstants;
import com.gems.auth.application.exceptions.GoogleSignInDisabledException;
import com.gems.auth.application.exceptions.UserDeactivatedException;
import com.gems.auth.application.gateway.ConsentGateway;
import com.gems.auth.application.gateway.EmailVerificationGateway;
import com.gems.auth.application.gateway.GoogleTokenVerifier;
import com.gems.auth.application.gateway.GoogleTokenVerifier.GoogleIdentity;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * One-click sign in / sign up with Google. Google proves ownership of the e-mail, so the account
 * is left verified and no verification mail is sent.
 */
public class GoogleSignInUseCase {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int NAME_MAX = 50;

  private final GoogleTokenVerifier google;
  private final UserGateway users;
  private final PasswordEncoderGateway encoder;
  private final JwtGateway jwt;
  private final EmailVerificationGateway verifications;
  private final ConsentGateway consents;
  private final EmailPreferencesUseCase preferences;
  private final String openInstitutionId;
  private final String policyVersion;

  public GoogleSignInUseCase(GoogleTokenVerifier google, UserGateway users, PasswordEncoderGateway encoder,
                             JwtGateway jwt, EmailVerificationGateway verifications, ConsentGateway consents,
                             EmailPreferencesUseCase preferences, String openInstitutionId, String policyVersion) {
    this.google = google;
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
    this.verifications = verifications;
    this.consents = consents;
    this.preferences = preferences;
    this.openInstitutionId = openInstitutionId;
    this.policyVersion = policyVersion;
  }

  public boolean isEnabled() {
    return google.isEnabled();
  }

  /**
   * @param guestUserId  id of the guest holding the request's session, or null when there is none
   * @param acceptPolicy data policy acceptance; only required when an account is created or claimed
   * @param tips         opt-in to reminders by e-mail
   */
  public Mono<LoginResponse> execute(String credential, boolean acceptPolicy, boolean tips, Long guestUserId) {
    if (!google.isEnabled()) return Mono.error(new GoogleSignInDisabledException());
    if (credential == null || credential.isBlank()) {
      return Mono.error(new IllegalArgumentException("Google credential is required"));
    }
    return google.verify(credential).flatMap(identity -> {
      Email email = new Email(identity.email().trim().toLowerCase());
      return users.findByEmail(email)
        // An existing account always wins, whatever its role: same e-mail, verified by Google.
        // If the request also carried a guest session, the guest's progress is NOT moved to this
        // account: the guest keeps its own data and only the existing account's session is returned.
        .flatMap(this::signIn)
        .switchIfEmpty(Mono.defer(() -> {
          if (!acceptPolicy) {
            return Mono.<LoginResponse>error(new IllegalArgumentException("You must accept the data processing policy"));
          }
          return guest(guestUserId)
            .flatMap(guest -> claim(guest, email, identity, tips))
            .switchIfEmpty(Mono.defer(() -> create(email, identity, tips)));
        }));
    });
  }

  private Mono<LoginResponse> signIn(User user) {
    if (Boolean.FALSE.equals(user.isActive())) {
      return Mono.error(new UserDeactivatedException(AuthAppConstants.USER_DEACTIVATED_MESSAGE));
    }
    return verifications.markVerified(user.getId().getValue(), LocalDateTime.now())
      .thenReturn(session(user));
  }

  /** The session's user, only if it is a live guest. */
  private Mono<User> guest(Long userId) {
    if (userId == null) return Mono.empty();
    return users.findById(new UserId(userId))
      .filter(u -> GuestAccessUseCase.isGuest(u.getEmail().getValue()) && !Boolean.FALSE.equals(u.isActive()));
  }

  /** Turns the guest into a regular account with the Google e-mail and name; progress and streak stay. */
  private Mono<LoginResponse> claim(User guest, Email email, GoogleIdentity identity, boolean tips) {
    return randomPassword()
      .map(encoded -> new User(guest.getId(), firstName(identity), lastName(identity), guest.getUsername(), email,
        new Password(encoded), UserRole.STUDENT, guest.getInstitutionId(),
        guest.getAvatarUrl() != null ? guest.getAvatarUrl() : identity.picture(),
        guest.getCreatedAt(), LocalDateTime.now(), guest.isActive()))
      .flatMap(claimed -> users.save(claimed).flatMap(saved -> users.findById(saved.getId())))
      .flatMap(saved -> afterCreation(saved, tips));
  }

  private Mono<LoginResponse> create(Email email, GoogleIdentity identity, boolean tips) {
    String username = "google." + HexFormat.of().formatHex(bytes(6));
    return randomPassword()
      .map(encoded -> new User(firstName(identity).getValue(), lastName(identity).getValue(), username,
        email.getValue(), encoded, UserRole.STUDENT, openInstitutionId, identity.picture()))
      .flatMap(newUser -> users.save(newUser).flatMap(saved -> users.findById(saved.getId())))
      .flatMap(saved -> afterCreation(saved, tips));
  }

  private Mono<LoginResponse> afterCreation(User saved, boolean tips) {
    Long id = saved.getId().getValue();
    return consents.record(id, ConsentGateway.DATA_POLICY, policyVersion, true)
      .then(tips ? consents.record(id, ConsentGateway.TIPS, policyVersion, true) : Mono.<Void>empty())
      .then(preferences.update(id, new EmailPreferences(true, tips)))
      .then(verifications.markVerified(id, LocalDateTime.now()))
      .thenReturn(session(saved));
  }

  /** Nobody knows or uses this password: the account is entered through Google or a password reset. */
  private Mono<String> randomPassword() {
    String raw = new Password("Gg1!" + HexFormat.of().formatHex(bytes(16))).getValue();
    return Blocking.offload(() -> encoder.encode(raw));
  }

  static UserName firstName(GoogleIdentity id) {
    String given = clean(id.givenName());
    if (given == null) {
      String name = clean(id.name());
      given = name == null ? null : name.split("\\s+", 2)[0];
    }
    if (given == null) given = id.email().substring(0, id.email().indexOf('@'));
    return new UserName(fit(given));
  }

  static UserName lastName(GoogleIdentity id) {
    String family = clean(id.familyName());
    if (family == null) {
      String name = clean(id.name());
      if (name != null && name.contains(" ")) family = name.split("\\s+", 2)[1];
    }
    return new UserName(fit(family == null ? "GEMS" : family));
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  /** Between 2 and 50 characters, as UserName requires. */
  private static String fit(String s) {
    String v = s.length() > NAME_MAX ? s.substring(0, NAME_MAX).trim() : s;
    return v.length() < 2 ? v + "." : v;
  }

  private LoginResponse session(User user) {
    String token = jwt.generateToken(user.getId().getValue(), user.getRole().name(), user.getInstitutionId(),
      user.getUpdatedAt().toString());
    return new LoginResponse(user.getId().getValue(), user.getFirstName().getValue(), user.getLastName().getValue(),
      user.getUsername(), user.getEmail().getValue(), user.getRole().name(), user.getInstitutionId(), user.getAvatarUrl(),
      user.isActive(), user.getCreatedAt(), user.getUpdatedAt(), token, user.mustChangePassword());
  }

  private static byte[] bytes(int n) {
    byte[] b = new byte[n];
    RANDOM.nextBytes(b);
    return b;
  }
}
