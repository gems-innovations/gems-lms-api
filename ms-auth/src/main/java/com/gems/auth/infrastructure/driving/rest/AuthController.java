package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.domain.exceptions.InvalidCredentialsException;
import com.gems.auth.domain.exceptions.UserNotFoundException;
import com.gems.auth.infrastructure.driving.rest.mapper.LoginMapper;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final LoginUseCase loginUseCase;

  public AuthController(LoginUseCase loginUseCase) {
    this.loginUseCase = loginUseCase;
  }

  @PostMapping("/login")
  public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
    LoginCommand command = LoginMapper.toCommand(request);

    return loginUseCase.execute(command)
      .map(ResponseEntity::ok)
      .onErrorResume(InvalidCredentialsException.class, error -> 
        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
      .onErrorResume(UserNotFoundException.class, error -> 
        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
      .onErrorResume(Exception.class, error -> 
        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
  }
}
