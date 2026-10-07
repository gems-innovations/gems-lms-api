package com.gems.auth.application;

import com.gems.auth.application.exceptions.PasswordChangeException;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.PasswordResetGateway;
import com.gems.auth.application.gateway.PasswordResetNotifier;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PasswordUseCasesTest {
  private final UserGateway userGateway = mock(UserGateway.class);
  private final PasswordEncoderGateway encoder = mock(PasswordEncoderGateway.class);
  private final PasswordResetGateway resetGateway = mock(PasswordResetGateway.class);
  private final PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);

  private final ChangePasswordUseCase changePassword = new ChangePasswordUseCase(userGateway, encoder);
  private final PasswordRecoveryUseCase recovery = new PasswordRecoveryUseCase(userGateway, resetGateway, notifier, encoder);

  private User user;

  @BeforeEach
  void setUp() {
    user = new User(7L, "Maria", "Lopez", "maria", "maria@example.com", "Hash123!x", UserRole.STUDENT, "inst-1", null);
    when(userGateway.findById(any(UserId.class))).thenReturn(Mono.just(user));
    when(userGateway.updatePassword(any(), anyString())).thenReturn(Mono.empty());
    when(encoder.matches("Current1!", "Hash123!x")).thenReturn(true);
    when(encoder.encode(anyString())).thenAnswer(inv -> "encoded:" + inv.getArgument(0));
  }

  @Test
  void changesThePasswordWhenTheCurrentOneMatches() {
    StepVerifier.create(changePassword.execute(7L, "Current1!", "Brand1New!")).verifyComplete();
    verify(userGateway).updatePassword(new UserId(7L), "encoded:Brand1New!");
  }

  @Test
  void rejectsAWrongCurrentPasswordOrTheSamePassword() {
    StepVerifier.create(changePassword.execute(7L, "Wrong1!x", "Brand1New!"))
      .expectErrorMatches(e -> e instanceof PasswordChangeException p
        && p.getCode().equals(PasswordChangeException.WRONG_CURRENT_PASSWORD))
      .verify();
    StepVerifier.create(changePassword.execute(7L, "Current1!", "Current1!"))
      .expectErrorMatches(e -> e instanceof PasswordChangeException p
        && p.getCode().equals(PasswordChangeException.SAME_PASSWORD))
      .verify();
    verify(userGateway, never()).updatePassword(any(), anyString());
  }

  @Test
  void aWeakNewPasswordIsRejected() {
    StepVerifier.create(changePassword.execute(7L, "Current1!", "short"))
      .expectError(IllegalArgumentException.class)
      .verify();
  }

  @Test
  void requestingAResetStoresOnlyTheHashAndSendsTheToken() {
    when(userGateway.findByEmail(new Email("maria@example.com"))).thenReturn(Mono.just(user));
    when(resetGateway.save(any(), anyString(), any())).thenReturn(Mono.empty());
    when(notifier.sendResetLink(anyString(), anyString(), anyString())).thenReturn(Mono.empty());

    StepVerifier.create(recovery.requestReset("maria@example.com")).verifyComplete();

    ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<LocalDateTime> expiry = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(notifier).sendResetLink(eq("maria@example.com"), eq("Maria"), token.capture());
    verify(resetGateway).save(eq(7L), storedHash.capture(), expiry.capture());
    assertEquals(PasswordRecoveryUseCase.hash(token.getValue()), storedHash.getValue());
    assertTrue(expiry.getValue().isAfter(LocalDateTime.now().plusMinutes(59)));
  }

  @Test
  void unknownOrInactiveAccountsGetNoLinkAndNoError() {
    when(userGateway.findByEmail(any())).thenReturn(Mono.empty());
    StepVerifier.create(recovery.requestReset("nobody@example.com")).verifyComplete();
    StepVerifier.create(recovery.requestReset("not-an-email")).verifyComplete();

    user.deactivate();
    when(userGateway.findByEmail(new Email("maria@example.com"))).thenReturn(Mono.just(user));
    StepVerifier.create(recovery.requestReset("maria@example.com")).verifyComplete();
    verifyNoInteractions(notifier, resetGateway);
  }

  @Test
  void aValidTokenSetsTheNewPasswordOnce() {
    String hash = PasswordRecoveryUseCase.hash("tok");
    when(resetGateway.findValidUser(eq(hash), any())).thenReturn(Mono.just(7L));
    when(resetGateway.markUsed(eq(hash), any())).thenReturn(Mono.just(true), Mono.just(false));

    StepVerifier.create(recovery.reset("tok", "Brand1New!")).verifyComplete();
    verify(userGateway).updatePassword(new UserId(7L), "encoded:Brand1New!");

    StepVerifier.create(recovery.reset("tok", "Brand1New!"))
      .expectErrorMatches(e -> e instanceof PasswordChangeException p
        && p.getCode().equals(PasswordChangeException.INVALID_RESET_TOKEN))
      .verify();
  }

  @Test
  void anUnknownOrExpiredTokenIsRejected() {
    when(resetGateway.findValidUser(anyString(), any())).thenReturn(Mono.empty());

    StepVerifier.create(recovery.reset("nope", "Brand1New!"))
      .expectError(PasswordChangeException.class)
      .verify();
    verify(userGateway, never()).updatePassword(any(), anyString());
  }
}
