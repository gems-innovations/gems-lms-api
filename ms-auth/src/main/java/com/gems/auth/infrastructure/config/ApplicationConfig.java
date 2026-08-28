package com.gems.auth.infrastructure.config;

import com.gems.auth.application.DisableUserUseCase;
import com.gems.auth.application.GetAllUsersUseCase;
import com.gems.auth.application.GetUserByIdUseCase;
import com.gems.auth.application.GetUsersByInstitutionUseCase;
import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.UpdateUserUseCase;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.infrastructure.driven.postgresql.IUserRepository;
import com.gems.auth.infrastructure.driven.postgresql.UserRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  public UserGateway userGateway(IUserRepository userRepository) {
    return new UserRepositoryAdapter(userRepository);
  }

  @Bean
  public RegisterUserUseCase registerUserUseCase(UserGateway userGateway, PasswordEncoderGateway passwordEncoderGateway) {
    return new RegisterUserUseCase(userGateway, passwordEncoderGateway);
  }

  @Bean
  public DisableUserUseCase disableUserUseCase(UserGateway userGateway) {
    return new DisableUserUseCase(userGateway);
  }

  @Bean
  public LoginUseCase loginUseCase(UserGateway userGateway,
                                   PasswordEncoderGateway passwordEncoderGateway,
                                   JwtGateway jwtGateway) {
    return new LoginUseCase(userGateway, passwordEncoderGateway, jwtGateway);
  }

  @Bean
  public GetUsersByInstitutionUseCase getUsersByInstitutionUseCase(UserGateway userGateway) {
    return new GetUsersByInstitutionUseCase(userGateway);
  }

  @Bean
  public GetUserByIdUseCase getUserByIdUseCase(UserGateway userGateway) {
    return new GetUserByIdUseCase(userGateway);
  }

  @Bean
  public GetAllUsersUseCase getAllUsersUseCase(UserGateway userGateway) {
    return new GetAllUsersUseCase(userGateway);
  }

  @Bean
  public UpdateUserUseCase updateUserUseCase(UserGateway userGateway) {
    return new UpdateUserUseCase(userGateway);
  }
}