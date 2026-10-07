package com.gems.auth.application;

import com.gems.auth.application.gateway.CourseNoticeNotifier;
import com.gems.auth.application.gateway.EmailPreferencesGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailPreferencesUseCaseTest {
  private final UserGateway users = mock(UserGateway.class);
  private final EmailPreferencesGateway preferences = mock(EmailPreferencesGateway.class);
  private final CourseNoticeNotifier notifier = mock(CourseNoticeNotifier.class);
  private final PreferenceTokens tokens = new PreferenceTokens("test-secret");
  private final EmailPreferencesUseCase useCase = new EmailPreferencesUseCase(users, preferences, notifier, tokens);

  private void user(long id, String email, EmailPreferences prefs) {
    when(users.findById(new UserId(id))).thenReturn(Mono.just(
      new User(id, "Ana", "Ruiz", "u" + id, email, "Hash123!x", UserRole.STUDENT, "inst-1", null)));
    when(preferences.find(id)).thenReturn(Mono.just(prefs));
  }

  @Test
  void preferenceTokensOnlyWorkForTheirOwnUser() {
    String token = tokens.create(7L);
    assertEquals(7L, tokens.userOf(token).orElseThrow());
    assertTrue(tokens.userOf("8" + token.substring(1)).isEmpty());
    assertTrue(tokens.userOf(token + "x").isEmpty());
    assertTrue(new PreferenceTokens("other-secret").userOf(token).isEmpty());
    assertTrue(tokens.userOf("nada").isEmpty());
  }

  @Test
  void noticesSkipGuestsAndUsersWhoTurnedThemOff() {
    user(1L, "ana@example.com", EmailPreferences.DEFAULTS);
    user(2L, "invitado-ab12@invitado.gems.lat", EmailPreferences.DEFAULTS);
    user(3L, "luis@example.com", new EmailPreferences(false, true));
    when(notifier.sendNotice(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
      .thenReturn(Mono.empty());

    StepVerifier.create(useCase.sendCourseNotice(List.of(1L, 2L, 3L, 1L), "Asunto", "Mensaje", "/learn/home", "Abrir"))
      .expectNext(1L).verifyComplete();

    verify(notifier).sendNotice(eq("ana@example.com"), anyString(), anyString(), anyString(), anyString(), anyString(),
      eq(tokens.create(1L)));
    verify(notifier, never()).sendNotice(eq("luis@example.com"), any(), any(), any(), any(), any(), any());
  }

  @Test
  void linksOutsideTheAppAreReplaced() {
    user(1L, "ana@example.com", EmailPreferences.DEFAULTS);
    when(notifier.sendNotice(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
      .thenReturn(Mono.empty());

    StepVerifier.create(useCase.sendCourseNotice(List.of(1L), "Asunto", "Mensaje", "//phishing.example", "Abrir"))
      .expectNext(1L).verifyComplete();

    verify(notifier).sendNotice(anyString(), anyString(), anyString(), anyString(), eq("/learn/home"), anyString(), anyString());
  }

  @Test
  void theLinkInTheEmailChangesPreferencesWithoutSigningIn() {
    when(preferences.save(eq(7L), any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.updateWithToken(tokens.create(7L), new EmailPreferences(false, false)))
      .expectNext(new EmailPreferences(false, false)).verifyComplete();
    StepVerifier.create(useCase.updateWithToken("7.falso", new EmailPreferences(false, false))).verifyComplete();

    verify(preferences).save(7L, new EmailPreferences(false, false));
  }
}
