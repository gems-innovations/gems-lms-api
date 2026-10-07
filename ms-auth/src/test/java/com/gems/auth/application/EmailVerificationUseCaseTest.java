package com.gems.auth.application;

import com.gems.auth.application.gateway.EmailVerificationGateway;
import com.gems.auth.application.gateway.EmailVerificationNotifier;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationUseCaseTest {
  private final UserGateway users = mock(UserGateway.class);
  private final EmailVerificationGateway gateway = mock(EmailVerificationGateway.class);
  private final EmailVerificationNotifier notifier = mock(EmailVerificationNotifier.class);
  private final EmailVerificationUseCase useCase = new EmailVerificationUseCase(users, gateway, notifier);

  private User user(String email) {
    return new User(7L, "Ana", "Ruiz", "ana", email, "Hash123!x", UserRole.STUDENT, "inst-1", null);
  }

  private void unverifiedUserNeverSent(String email) {
    when(users.findById(new UserId(7L))).thenReturn(Mono.just(user(email)));
    when(gateway.isVerified(7L)).thenReturn(Mono.just(false));
    when(gateway.lastSentAt(7L)).thenReturn(Mono.empty());
    when(gateway.issue(any(), anyString(), any(), any())).thenReturn(Mono.empty());
    when(notifier.sendVerification(anyString(), anyString(), anyString())).thenReturn(Mono.empty());
  }

  @Test
  void sendsALinkAndStoresOnlyItsHash() {
    unverifiedUserNeverSent("ana@example.com");

    StepVerifier.create(useCase.send(7L)).verifyComplete();

    verify(notifier).sendVerification(org.mockito.ArgumentMatchers.eq("ana@example.com"),
      org.mockito.ArgumentMatchers.eq("Ana"), org.mockito.ArgumentMatchers.argThat(t -> t.length() >= 40));
    verify(gateway).issue(org.mockito.ArgumentMatchers.eq(7L),
      org.mockito.ArgumentMatchers.argThat(h -> h.length() == 64), any(), any());
  }

  @Test
  void doesNotSendToGuests() {
    when(users.findById(new UserId(7L))).thenReturn(Mono.just(user("invitado-ab12@invitado.gems.lat")));

    StepVerifier.create(useCase.send(7L)).verifyComplete();

    verify(notifier, never()).sendVerification(anyString(), anyString(), anyString());
  }

  @Test
  void doesNotSendAgainWhenAlreadyVerified() {
    when(users.findById(new UserId(7L))).thenReturn(Mono.just(user("ana@example.com")));
    when(gateway.isVerified(7L)).thenReturn(Mono.just(true));

    StepVerifier.create(useCase.send(7L)).verifyComplete();

    verify(notifier, never()).sendVerification(anyString(), anyString(), anyString());
  }

  @Test
  void doesNotResendWithinAMinute() {
    unverifiedUserNeverSent("ana@example.com");
    when(gateway.lastSentAt(7L)).thenReturn(Mono.just(LocalDateTime.now().minusSeconds(10)));

    StepVerifier.create(useCase.send(7L)).verifyComplete();

    verify(notifier, never()).sendVerification(anyString(), anyString(), anyString());
  }

  @Test
  void confirmsOnlyValidTokens() {
    when(gateway.confirm(anyString(), any())).thenReturn(Mono.just(7L), Mono.empty());

    StepVerifier.create(useCase.confirm("good-token")).expectNext(true).verifyComplete();
    StepVerifier.create(useCase.confirm("used-token")).expectNext(false).verifyComplete();
    StepVerifier.create(useCase.confirm(" ")).expectNext(false).verifyComplete();
  }
}
