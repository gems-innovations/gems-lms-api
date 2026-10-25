package com.gems.auth.application;

import com.gems.auth.application.exceptions.GoogleSignInDisabledException;
import com.gems.auth.application.exceptions.InvalidCredentialsException;
import com.gems.auth.application.exceptions.UserDeactivatedException;
import com.gems.auth.application.gateway.ConsentGateway;
import com.gems.auth.application.gateway.EmailVerificationGateway;
import com.gems.auth.application.gateway.GoogleTokenVerifier;
import com.gems.auth.application.gateway.GoogleTokenVerifier.GoogleIdentity;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleSignInUseCaseTest {
  private static final String POLICY = "2026-10";
  private static final String OPEN = "gems-abierto";

  private final GoogleTokenVerifier google = mock(GoogleTokenVerifier.class);
  private final UserGateway users = mock(UserGateway.class);
  private final PasswordEncoderGateway encoder = mock(PasswordEncoderGateway.class);
  private final JwtGateway jwt = mock(JwtGateway.class);
  private final EmailVerificationGateway verifications = mock(EmailVerificationGateway.class);
  private final ConsentGateway consents = mock(ConsentGateway.class);
  private final EmailPreferencesUseCase preferences = mock(EmailPreferencesUseCase.class);
  private GoogleSignInUseCase useCase;

  private final GoogleIdentity ana = new GoogleIdentity("Ana@Example.com", "Ana Ruiz", "Ana", "Ruiz", "https://pic/ana");

  @BeforeEach
  void setUp() {
    useCase = new GoogleSignInUseCase(google, users, encoder, jwt, verifications, consents, preferences, OPEN, POLICY);
    when(google.isEnabled()).thenReturn(true);
    when(google.verify("good")).thenReturn(Mono.just(ana));
    when(encoder.encode(anyString())).thenReturn("Hash123!x");
    when(jwt.generateToken(anyLong(), anyString(), any(), anyString())).thenReturn("gems-token");
    when(verifications.markVerified(anyLong(), any())).thenReturn(Mono.empty());
    when(consents.record(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(Mono.empty());
    when(preferences.update(anyLong(), any())).thenReturn(Mono.just(EmailPreferences.DEFAULTS));
    when(users.findByEmail(any())).thenReturn(Mono.empty());
    when(users.save(any())).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      return Mono.just(u.getId() != null ? u
        : new User(new UserId(50L), u.getFirstName(), u.getLastName(), u.getUsername(), u.getEmail(), u.getPassword(),
          u.getRole(), u.getInstitutionId(), u.getAvatarUrl(), u.getCreatedAt(), u.getUpdatedAt(), true));
    });
    when(users.findById(new UserId(50L))).thenAnswer(inv -> Mono.just(stored.get()));
  }

  private final java.util.concurrent.atomic.AtomicReference<User> stored = new java.util.concurrent.atomic.AtomicReference<>();

  private User existing(String email, UserRole role, String institution) {
    return new User(7L, "Ana", "Ruiz", "ana", email, "Hash123!x", role, institution, null);
  }

  @Test
  void signsInAnExistingAccountAndMarksItVerified() {
    when(users.findByEmail(new Email("ana@example.com"))).thenReturn(Mono.just(existing("ana@example.com", UserRole.INSTRUCTOR, "inst-1")));

    StepVerifier.create(useCase.execute("good", false, false, null))
      .assertNext(r -> {
        assertEquals(7L, r.userId());
        assertEquals("INSTRUCTOR", r.role());
        assertEquals("gems-token", r.token());
      })
      .verifyComplete();

    verify(verifications).markVerified(eq(7L), any());
    verify(consents, never()).record(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean());
    verify(users, never()).save(any());
  }

  @Test
  void rejectsADeactivatedAccount() {
    User off = existing("ana@example.com", UserRole.STUDENT, OPEN);
    off.deactivate();
    when(users.findByEmail(new Email("ana@example.com"))).thenReturn(Mono.just(off));

    StepVerifier.create(useCase.execute("good", true, false, null))
      .expectError(UserDeactivatedException.class).verify();
    verify(verifications, never()).markVerified(anyLong(), any());
  }

  @Test
  void createsAStudentInTheOpenInstitution() {
    stored.set(new User(new UserId(50L), new com.gems.auth.domain.values.UserName("Ana"),
      new com.gems.auth.domain.values.UserName("Ruiz"), "google.x", new Email("ana@example.com"),
      new com.gems.auth.domain.values.Password("Hash123!x"), UserRole.STUDENT, OPEN, "https://pic/ana",
      java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), true));

    StepVerifier.create(useCase.execute("good", true, true, null))
      .assertNext(r -> {
        assertEquals(50L, r.userId());
        assertEquals("STUDENT", r.role());
        assertEquals(OPEN, r.institutionId());
      })
      .verifyComplete();

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    assertEquals("ana@example.com", saved.getValue().getEmail().getValue());
    assertEquals("Ana", saved.getValue().getFirstName().getValue());
    assertEquals("Ruiz", saved.getValue().getLastName().getValue());
    assertEquals("https://pic/ana", saved.getValue().getAvatarUrl());
    assertTrue(saved.getValue().getUsername().startsWith("google."));
    verify(consents).record(50L, ConsentGateway.DATA_POLICY, POLICY, true);
    verify(consents).record(50L, ConsentGateway.TIPS, POLICY, true);
    verify(verifications).markVerified(eq(50L), any());
  }

  @Test
  void splitsTheFullNameWhenGivenAndFamilyNamesAreMissing() {
    when(google.verify("noparts")).thenReturn(Mono.just(new GoogleIdentity("x@example.com", "Luis Perez Gomez", null, null, null)));
    stored.set(existing("x@example.com", UserRole.STUDENT, OPEN));
    StepVerifier.create(useCase.execute("noparts", true, false, null)).expectNextCount(1).verifyComplete();

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    assertEquals("Luis", saved.getValue().getFirstName().getValue());
    assertEquals("Perez Gomez", saved.getValue().getLastName().getValue());
    verify(consents, never()).record(anyLong(), eq(ConsentGateway.TIPS), anyString(), org.mockito.ArgumentMatchers.anyBoolean());
  }

  @Test
  void convertsTheGuestKeepingItsIdAndUsername() {
    User guest = new User(31L, "Invitado", "GEMS", "invitado.ab12", "invitado-ab12" + GuestAccessUseCase.GUEST_DOMAIN,
      "Hash123!x", UserRole.STUDENT, OPEN, null);
    // First read: the guest as it was; second read (after saving): what was stored.
    when(users.findById(new UserId(31L))).thenReturn(Mono.just(guest)).thenAnswer(inv -> Mono.just(stored.get()));
    org.mockito.Mockito.doAnswer(inv -> {
      stored.set(inv.getArgument(0));
      return Mono.just(stored.get());
    }).when(users).save(any());

    StepVerifier.create(useCase.execute("good", true, false, 31L))
      .assertNext(r -> {
        assertEquals(31L, r.userId());
        assertEquals("ana@example.com", r.email());
        assertEquals("Ana", r.firstName());
      })
      .verifyComplete();

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    assertEquals("invitado.ab12", saved.getValue().getUsername());
    assertEquals(guest.getCreatedAt(), saved.getValue().getCreatedAt());
    verify(consents).record(31L, ConsentGateway.DATA_POLICY, POLICY, true);
    verify(verifications).markVerified(eq(31L), any());
  }

  @Test
  void guestWithAnEmailThatAlreadyExistsSignsInToThatAccountAndLeavesTheGuestUntouched() {
    when(users.findByEmail(new Email("ana@example.com"))).thenReturn(Mono.just(existing("ana@example.com", UserRole.STUDENT, OPEN)));

    StepVerifier.create(useCase.execute("good", false, false, 31L))
      .assertNext(r -> assertEquals(7L, r.userId()))
      .verifyComplete();

    verify(users, never()).save(any());
    verify(users, never()).findById(new UserId(31L));
  }

  @Test
  void requiresThePolicyToCreateOrClaim() {
    StepVerifier.create(useCase.execute("good", false, false, null))
      .expectErrorMatches(e -> e instanceof IllegalArgumentException && e.getMessage().contains("policy")).verify();
    StepVerifier.create(useCase.execute("good", false, false, 31L))
      .expectError(IllegalArgumentException.class).verify();
    verify(users, never()).save(any());
    verify(consents, never()).record(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean());
  }

  @Test
  void anInvalidTokenIsRejected() {
    when(google.verify("bad")).thenReturn(Mono.error(new InvalidCredentialsException("Invalid Google credential")));

    StepVerifier.create(useCase.execute("bad", true, false, null))
      .expectError(InvalidCredentialsException.class).verify();
    verify(users, never()).findByEmail(any());
  }

  @Test
  void isOffWhenGoogleIsNotConfigured() {
    when(google.isEnabled()).thenReturn(false);

    StepVerifier.create(useCase.execute("good", true, false, null))
      .expectError(GoogleSignInDisabledException.class).verify();
    verify(google, never()).verify(anyString());
    assertNotEquals(true, useCase.isEnabled());
  }
}
