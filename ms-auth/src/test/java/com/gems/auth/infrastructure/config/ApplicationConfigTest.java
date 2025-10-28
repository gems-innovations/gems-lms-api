package com.gems.auth.infrastructure.config;

import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.infrastructure.driven.postgresql.IUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ApplicationConfig Tests")
class ApplicationConfigTest {

  @Test
  @DisplayName("Should create UserGateway bean")
  void shouldCreateUserGatewayBean() {
    ApplicationConfig config = new ApplicationConfig();
    IUserRepository userRepository = mock(IUserRepository.class);

    UserGateway gateway = config.userGateway(userRepository);

    assertNotNull(gateway);
  }

  @Test
  @DisplayName("Should create RegisterUserUseCase bean")
  void shouldCreateRegisterUserUseCaseBean() {
    ApplicationConfig config = new ApplicationConfig();
    UserGateway userGateway = mock(UserGateway.class);
    PasswordEncoderGateway passwordEncoderGateway = mock(PasswordEncoderGateway.class);

    RegisterUserUseCase useCase = config.registerUserUseCase(userGateway, passwordEncoderGateway);

    assertNotNull(useCase);
  }

  @Test
  @DisplayName("Should create LoginUseCase bean")
  void shouldCreateLoginUseCaseBean() {
    ApplicationConfig config = new ApplicationConfig();
    UserGateway userGateway = mock(UserGateway.class);
    PasswordEncoderGateway passwordEncoderGateway = mock(PasswordEncoderGateway.class);
    JwtGateway jwtGateway = mock(JwtGateway.class);

    LoginUseCase loginUseCase = config.loginUseCase(userGateway, passwordEncoderGateway, jwtGateway);

    assertNotNull(loginUseCase);
  }
}
