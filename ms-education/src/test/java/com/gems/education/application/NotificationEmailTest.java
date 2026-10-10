package com.gems.education.application;

import com.gems.education.application.gateway.EmailNoticeGateway;
import com.gems.education.application.gateway.NotificationGateway;
import com.gems.education.domain.entities.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Every in-app notice that has a clear recipient is also handed over to be e-mailed. */
class NotificationEmailTest {
  private final NotificationGateway gateway = mock(NotificationGateway.class);
  private final EmailNoticeGateway email = mock(EmailNoticeGateway.class);
  private final NotificationUseCase useCase = new NotificationUseCase(gateway, email);

  @BeforeEach
  void setUp() {
    when(gateway.existsFor(any(), anyString(), any())).thenReturn(Mono.just(false));
    when(gateway.save(any())).thenAnswer(inv -> Mono.just((Notification) inv.getArgument(0)));
    when(email.send(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.empty());
    when(email.sendToStaff(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.empty());
  }

  @Test
  void gradedWorkIsEmailedToTheStudent() {
    StepVerifier.create(useCase.submissionGraded("inst-1", 7L, 9L, "Álgebra", 3L, 88)).expectNextCount(1).verifyComplete();
    verify(email).send(eq(List.of(7L)), contains("Álgebra"), contains("88"), eq("/learn/courses/9/grades"), anyString());
  }

  @Test
  void aNewSubmissionIsEmailedToTheStaff() {
    StepVerifier.create(useCase.submissionReceived("inst-1", 9L, "Álgebra", 3L)).expectNextCount(1).verifyComplete();
    verify(email).sendToStaff(eq("inst-1"), contains("Álgebra"), anyString(), anyString(), anyString());
  }

  @Test
  void aFailedBellNoticeNeverBreaksTheEnrolment() {
    org.mockito.Mockito.doReturn(Mono.error(new IllegalStateException("db down"))).when(gateway).save(any());
    StepVerifier.create(useCase.enrolled(7L, 9L, "Álgebra", false)).verifyComplete();
    verify(email).send(eq(List.of(7L)), contains("Álgebra"), anyString(), eq("/learn/courses/9"), anyString());
  }

  @Test
  void enrollingAgainInTheSameCourseDoesNotRepeatTheWelcome() {
    when(gateway.existsFor(7L, Notification.ENROLLED, 9L)).thenReturn(Mono.just(true));
    StepVerifier.create(useCase.enrolled(7L, 9L, "Álgebra", false)).verifyComplete();
    org.mockito.Mockito.verify(gateway, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(email);
  }

  @Test
  void aCertificateIsEmailedToTheStudent() {
    StepVerifier.create(useCase.certificateIssued(7L, "Álgebra", "ABC123")).verifyComplete();
    verify(email).send(eq(List.of(7L)), contains("Álgebra"), anyString(), anyString(), anyString());
  }
}
