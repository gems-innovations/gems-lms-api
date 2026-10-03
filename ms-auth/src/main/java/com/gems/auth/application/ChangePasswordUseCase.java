package com.gems.auth.application;

import com.gems.auth.application.exceptions.PasswordChangeException;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

/** A signed-in user replaces their password (also clears the "must change password" flag). */
public class ChangePasswordUseCase {
  private final UserGateway userGateway;
  private final PasswordEncoderGateway passwordEncoderGateway;

  public ChangePasswordUseCase(UserGateway userGateway, PasswordEncoderGateway passwordEncoderGateway) {
    this.userGateway = userGateway;
    this.passwordEncoderGateway = passwordEncoderGateway;
  }

  public Mono<Void> execute(Long userId, String currentPassword, String newPassword) {
    return userGateway.findById(new UserId(userId))
      .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + userId)))
      .flatMap(user -> {
        if (currentPassword == null || !passwordEncoderGateway.matches(currentPassword, user.getPassword().getValue())) {
          return Mono.error(new PasswordChangeException(PasswordChangeException.WRONG_CURRENT_PASSWORD,
            "The current password is not correct"));
        }
        if (currentPassword.equals(newPassword)) {
          return Mono.error(new PasswordChangeException(PasswordChangeException.SAME_PASSWORD,
            "The new password must be different from the current one"));
        }
        String validated = new Password(newPassword).getValue();
        return userGateway.updatePassword(user.getId(), passwordEncoderGateway.encode(validated));
      });
  }
}
