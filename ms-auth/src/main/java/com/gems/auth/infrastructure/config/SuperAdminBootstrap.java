package com.gems.auth.infrastructure.config;

import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.values.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Registration requires an administrator, so the very first SUPER_ADMIN is created here at
 * startup from BOOTSTRAP_SUPERADMIN_EMAIL / BOOTSTRAP_SUPERADMIN_PASSWORD. Nothing happens
 * when the variables are not set or the user already exists.
 */
@Component
public class SuperAdminBootstrap implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(SuperAdminBootstrap.class);

  private final UserGateway userGateway;
  private final RegisterUserUseCase registerUserUseCase;

  @Value("${bootstrap.super-admin.email:}")
  private String email;

  @Value("${bootstrap.super-admin.password:}")
  private String password;

  public SuperAdminBootstrap(UserGateway userGateway, RegisterUserUseCase registerUserUseCase) {
    this.userGateway = userGateway;
    this.registerUserUseCase = registerUserUseCase;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (email.isBlank() || password.isBlank()) return;

    Boolean exists = userGateway.existsByEmail(new Email(email)).block();
    if (Boolean.TRUE.equals(exists)) return;

    registerUserUseCase.execute(new RegisterUserCommand(
      "Super", "Admin", "superadmin", email, password, "SUPER_ADMIN", null
    )).block();
    LOG.info("Created bootstrap super admin {}", email);
  }
}
