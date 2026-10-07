package com.gems.auth.application;

import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.gateway.LearningDataRemovalGateway;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteUserUseCaseTest {
  private final UserGateway gateway = mock(UserGateway.class);
  private final LearningDataRemovalGateway learningData = mock(LearningDataRemovalGateway.class);
  private final DeleteUserUseCase useCase = new DeleteUserUseCase(gateway, learningData);

  @Test
  void deletesTheAccount() {
    User user = new User(7L, "Ana", "Ruiz", "ana", "ana@example.com", "Hash123!x", UserRole.STUDENT, "inst-1", null);
    when(gateway.findById(new UserId(7L))).thenReturn(Mono.just(user));
    when(gateway.deleteById(any())).thenReturn(Mono.empty());
    when(learningData.remove(any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.execute(new UserId(7L))).verifyComplete();
    verify(gateway).deleteById(new UserId(7L));
    var ordered = inOrder(learningData, gateway);
    ordered.verify(learningData).remove(new UserId(7L));
    ordered.verify(gateway).deleteById(new UserId(7L));
    verify(gateway, never()).save(any());
  }

  @Test
  void unknownUsersAreNotFound() {
    when(gateway.findById(any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.execute(new UserId(8L))).expectError(UserNotFoundException.class).verify();
  }

  @Test
  void preservesTheAccountIfLearningDataRemovalFails() {
    User user = new User(7L, "Ana", "Ruiz", "ana", "ana@example.com", "Hash123!x", UserRole.STUDENT, "inst-1", null);
    when(gateway.findById(new UserId(7L))).thenReturn(Mono.just(user));
    when(learningData.remove(any())).thenReturn(Mono.error(new IllegalStateException("Education unavailable")));
    StepVerifier.create(useCase.execute(new UserId(7L))).expectError(IllegalStateException.class).verify();
    verify(gateway, never()).deleteById(any());
  }
}
