package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUsersByInstitutionUseCaseTest {

  @Mock
  private UserGateway userGateway;

  @InjectMocks
  private GetUsersByInstitutionUseCase getUsersByInstitutionUseCase;

  @Test
  void shouldReturnUsersInInstitution() {
    // Given
    String institutionId = "inst-123";
    User user1 = new User(1L, "User One", "one@example.com", "SecurePass123!", UserRole.STUDENT, institutionId);
    User user2 = new User(2L, "User Two", "two@example.com", "SecurePass123!", UserRole.TEACHER, institutionId);

    when(userGateway.findByInstitutionId(institutionId)).thenReturn(Flux.just(user1, user2));

    // When
    Flux<UserResponse> result = getUsersByInstitutionUseCase.execute(institutionId);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.userId().equals(1L) &&
          response.name().equals("User One") &&
          response.role().equals("STUDENT") &&
          response.institutionId().equals(institutionId)
      )
      .expectNextMatches(response ->
        response.userId().equals(2L) &&
          response.name().equals("User Two") &&
          response.role().equals("TEACHER") &&
          response.institutionId().equals(institutionId)
      )
      .verifyComplete();

    verify(userGateway, times(1)).findByInstitutionId(institutionId);
  }
}
