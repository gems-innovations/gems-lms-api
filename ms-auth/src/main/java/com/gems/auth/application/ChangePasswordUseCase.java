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
        if (currentPassword == null) return Mono.<Void>error(wrongCurrentPassword());
        return Blocking.offload(() -> passwordEncoderGateway.matches(currentPassword, user.getPassword().getValue()))
          .map(Boolean.TRUE::equals)
          .defaultIfEmpty(false)
          .flatMap(matches -> {
            if (!matches) return Mono.<Void>error(wrongCurrentPassword());
            if (currentPassword.equals(newPassword)) {
              return Mono.<Void>error(new PasswordChangeException(PasswordChangeException.SAME_PASSWORD,
                "The new password must be different from the current one"));
            }
            String validated = new Password(newPassword).getValue();
            return Blocking.offload(() -> passwordEncoderGateway.encode(validated))
              .flatMap(encoded -> userGateway.updatePassword(user.getId(), encoded));
          });
      });
  }

  private static PasswordChangeException wrongCurrentPassword() {
    return new PasswordChangeException(PasswordChangeException.WRONG_CURRENT_PASSWORD,
      "The current password is not correct");
  }
}
