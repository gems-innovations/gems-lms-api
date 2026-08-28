package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.DisableUserUseCase;
import com.gems.auth.application.GetAllUsersUseCase;
import com.gems.auth.application.GetUserByIdUseCase;
import com.gems.auth.application.GetUsersByInstitutionUseCase;
import com.gems.auth.application.UpdateUserUseCase;
import com.gems.auth.application.command.UpdateUserCommand;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import com.gems.auth.infrastructure.driving.rest.request.UpdateUserRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(RestConstants.USERS_API_BASE_PATH)
public class UserController {
  private final DisableUserUseCase disableUserUseCase;
  private final GetUsersByInstitutionUseCase getUsersByInstitutionUseCase;
  private final GetUserByIdUseCase getUserByIdUseCase;
  private final GetAllUsersUseCase getAllUsersUseCase;
  private final UpdateUserUseCase updateUserUseCase;

  public UserController(DisableUserUseCase disableUserUseCase,
                        GetUsersByInstitutionUseCase getUsersByInstitutionUseCase,
                        GetUserByIdUseCase getUserByIdUseCase,
                        GetAllUsersUseCase getAllUsersUseCase,
                        UpdateUserUseCase updateUserUseCase) {
    this.disableUserUseCase = disableUserUseCase;
    this.getUsersByInstitutionUseCase = getUsersByInstitutionUseCase;
    this.getUserByIdUseCase = getUserByIdUseCase;
    this.getAllUsersUseCase = getAllUsersUseCase;
    this.updateUserUseCase = updateUserUseCase;
  }

  @GetMapping
  public Mono<ResponseEntity<Flux<UserResponse>>> getAllUsers() {
    return Mono.just(ResponseEntity.ok(getAllUsersUseCase.execute()));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable("id") Long id) {
    return getUserByIdUseCase.execute(id)
      .map(ResponseEntity::ok)
      .onErrorResume(UserNotFoundException.class,
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
      .onErrorResume(Exception.class,
        ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<UserResponse>> updateUser(@PathVariable("id") Long id,
                                                       @Valid @RequestBody UpdateUserRequest request) {
    UpdateUserCommand command = new UpdateUserCommand(id, request.getName(), request.getRole(), request.getInstitutionId());
    return updateUserUseCase.execute(command)
      .map(ResponseEntity::ok)
      .onErrorResume(UserNotFoundException.class,
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
      .onErrorResume(Exception.class,
        ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> disableUser(@PathVariable("id") Long id) {
    UserId userId = new UserId(id);
    return disableUserUseCase.execute(userId)
      .then(Mono.just(ResponseEntity.noContent().<Void>build()))
      .onErrorResume(UserNotFoundException.class,
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
      .onErrorResume(Exception.class,
        ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
  }

  @GetMapping("/institution/{instId}")
  public Mono<ResponseEntity<Flux<UserResponse>>> getUsersByInstitution(@PathVariable("instId") String instId) {
    return Mono.just(ResponseEntity.ok(getUsersByInstitutionUseCase.execute(instId)));
  }
}
