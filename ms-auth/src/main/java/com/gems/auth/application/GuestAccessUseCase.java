package com.gems.auth.application;

import com.gems.auth.application.exceptions.UserAlreadyExistsException;
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
 * Access without registering: a visitor who starts a free course becomes a guest student of the
 * open institution right away (no email, no password to remember). Their progress is stored like
 * anyone else's; claiming the account later turns that same user into a regular one, so nothing
 * has to be moved.
 */
public class GuestAccessUseCase {
  /** Guest accounts use this email domain; it marks them as guests until they are claimed. */
  public static final String GUEST_DOMAIN = "@invitado.gems.lat";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserGateway users;
  private final PasswordEncoderGateway encoder;
  private final JwtGateway jwt;
  private final String openInstitutionId;

  public GuestAccessUseCase(UserGateway users, PasswordEncoderGateway encoder, JwtGateway jwt, String openInstitutionId) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
    this.openInstitutionId = openInstitutionId;
  }

  public static boolean isGuest(String email) {
    return email != null && email.endsWith(GUEST_DOMAIN);
  }

  public Mono<LoginResponse> start(String nickname) {
    String id = HexFormat.of().formatHex(bytes(6));
    String name = nickname == null || nickname.trim().length() < 2 ? "Invitado" : nickname.trim();
    if (name.length() > 40) name = name.substring(0, 40);
    // Nobody signs in with this password: the guest keeps the session token until they claim the account.
    String password = "Gg1!" + HexFormat.of().formatHex(bytes(12));
    User guest = new User(name, "GEMS", "invitado." + id, "invitado-" + id + GUEST_DOMAIN,
      encoder.encode(new Password(password).getValue()), UserRole.STUDENT, openInstitutionId, null);
    // Read back so the token carries exactly the session revision stored in the database.
    return users.save(guest).flatMap(saved -> users.findById(saved.getId())).map(this::session);
  }

  /** Turns the guest into a regular account with its own email and password; progress stays. */
  public Mono<LoginResponse> claim(Long userId, String firstName, String lastName, String email, String password) {
    Email newEmail = new Email(email.trim().toLowerCase());
    Password newPassword = new Password(password);
    return users.findById(new UserId(userId))
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Guest not found")))
      .flatMap(guest -> {
        if (!isGuest(guest.getEmail().getValue())) {
          return Mono.error(new IllegalArgumentException("This account is already registered"));
        }
        return users.existsByEmail(newEmail).flatMap(taken -> {
          if (Boolean.TRUE.equals(taken)) {
            return Mono.<LoginResponse>error(new UserAlreadyExistsException("An account with this email already exists"));
          }
          User claimed = new User(guest.getId(), new UserName(firstName.trim()), new UserName(lastName.trim()),
            guest.getUsername(), newEmail, new Password(encoder.encode(newPassword.getValue())), UserRole.STUDENT,
            guest.getInstitutionId(), guest.getAvatarUrl(), guest.getCreatedAt(), LocalDateTime.now(), guest.isActive());
          return users.save(claimed).flatMap(saved -> users.findById(saved.getId())).map(this::session);
        });
      });
  }

  private LoginResponse session(User user) {
    String token = jwt.generateToken(user.getId().getValue(), user.getRole().name(), user.getInstitutionId(),
      user.getUpdatedAt().toString());
    return new LoginResponse(user.getId().getValue(), user.getFirstName().getValue(), user.getLastName().getValue(),
      user.getUsername(), user.getEmail().getValue(), user.getRole().name(), user.getInstitutionId(), user.getAvatarUrl(),
      user.isActive(), user.getCreatedAt(), user.getUpdatedAt(), token, false);
  }

  private static byte[] bytes(int n) {
    byte[] b = new byte[n];
    RANDOM.nextBytes(b);
    return b;
  }
}
