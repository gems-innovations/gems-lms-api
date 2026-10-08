package com.gems.auth.application;

import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.constants.AuthAppConstants;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.application.exceptions.UserAlreadyExistsException;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserRole;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;

public class RegisterUserUseCase {
  private static final String PASSWORD_CHARS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserGateway userGateway;
  private final PasswordEncoderGateway passwordEncoderGateway;

  public RegisterUserUseCase(UserGateway userGateway, PasswordEncoderGateway passwordEncoderGateway) {
    this.userGateway = userGateway;
    this.passwordEncoderGateway = passwordEncoderGateway;
  }

  public Mono<UserResponse> execute(RegisterUserCommand command) {
    return userGateway.existsByEmail(new Email(command.email()))
      .flatMap(exists -> {
        if (Boolean.TRUE.equals(exists)) {
          return Mono.error(new UserAlreadyExistsException(
            String.format(AuthAppConstants.USER_ALREADY_EXISTS_MESSAGE, command.email())
          ));
        }

        boolean passwordWasGenerated = command.password() == null || command.password().isBlank();
        String rawPassword = passwordWasGenerated ? generateTemporaryPassword() : command.password();
        String userPassword = new Password(rawPassword).getValue();

        return Mono.zip(Blocking.offload(() -> passwordEncoderGateway.encode(userPassword)),
            resolveUsername(command.username(), command.firstName(), command.lastName()))
          .flatMap(encodedAndUsername -> {
            String encodedPassword = encodedAndUsername.getT1();
            String username = encodedAndUsername.getT2();
            User user = new User(
              command.firstName(),
              command.lastName(),
              username,
              command.email(),
              encodedPassword,
              UserRole.fromString(command.role()),
              command.institutionId(),
              null
            );
            user.setMustChangePassword(passwordWasGenerated);

            return userGateway.save(user)
              .map(savedUser -> new UserResponse(
                savedUser.getId().getValue(),
                savedUser.getFirstName().getValue(),
                savedUser.getLastName().getValue(),
                savedUser.getUsername(),
                savedUser.getEmail().getValue(),
                savedUser.getRole().name(),
                savedUser.getInstitutionId(),
                savedUser.getAvatarUrl(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt(),
                savedUser.isActive(),
                passwordWasGenerated ? rawPassword : null
              ));
          });
      });
  }

  private Mono<String> resolveUsername(String requestedUsername, String firstName, String lastName) {
    String base = (requestedUsername != null && !requestedUsername.isBlank())
      ? requestedUsername.trim().toLowerCase()
      : (firstName + "." + lastName).trim().toLowerCase().replaceAll("[^a-z0-9.]", "");

    return findAvailableUsername(base, 0);
  }

  private Mono<String> findAvailableUsername(String base, int attempt) {
    String candidate = attempt == 0 ? base : base + attempt;
    return userGateway.existsByUsername(candidate)
      .flatMap(exists -> Boolean.TRUE.equals(exists)
        ? findAvailableUsername(base, attempt + 1)
        : Mono.just(candidate));
  }

  private String generateTemporaryPassword() {
    StringBuilder sb = new StringBuilder("Aa1!");
    for (int i = 0; i < 8; i++) {
      sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }
}
