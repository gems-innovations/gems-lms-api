package com.gems.education.application;

import com.gems.education.application.CourseCommunityUseCase.Course;
import com.gems.education.application.gateway.AnnouncementGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.ForumGateway;
import com.gems.education.domain.entities.Announcement;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.ForumPost;
import com.gems.education.domain.entities.ForumThread;
import com.gems.shared.security.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseCommunityUseCaseTest {
  private static final LocalDateTime T = LocalDateTime.of(2026, 10, 3, 12, 0);
  private static final Course COURSE = new Course(1L, "inst-1", "Docker");
  private static final Actor TEACHER = new Actor(3L, "Andrés Torres", "INSTRUCTOR", true);
  private static final Actor ANA = new Actor(5L, "Ana Ruiz", "STUDENT", false);
  private static final Actor LUIS = new Actor(6L, "Luis Gil", "STUDENT", false);

  private final AnnouncementGateway announcements = mock(AnnouncementGateway.class);
  private final ForumGateway forum = mock(ForumGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final NotificationUseCase notifications = mock(NotificationUseCase.class);
  private final CourseCommunityUseCase useCase = new CourseCommunityUseCase(announcements, forum, enrollments, notifications);

  @BeforeEach
  void setUp() {
    when(announcements.save(any())).thenAnswer(inv -> {
      Announcement a = inv.getArgument(0);
      return Mono.just(new Announcement(a.id() == null ? 10L : a.id(), a.courseId(), a.authorId(), a.authorName(),
        a.title(), a.body(), a.pinned(), a.createdAt(), a.updatedAt()));
    });
    when(forum.saveThread(any())).thenAnswer(inv -> {
      ForumThread t = inv.getArgument(0);
      return Mono.just(withId(t, t.id() == null ? 20L : t.id()));
    });
    when(forum.savePost(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(forum.findThread(20L)).thenReturn(Mono.just(thread(false)));
    when(enrollments.findByCourseId(1L)).thenReturn(Flux.just(enrollment(5L), enrollment(6L)));
    when(notifications.announcement(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    when(notifications.forumThread(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    when(notifications.forumReply(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());
  }

  @Test
  void staffAnnounceAndEveryEnrolledStudentIsNotified() {
    StepVerifier.create(useCase.announce(TEACHER, COURSE, " Examen ", "El viernes", true)).expectNextCount(1).verifyComplete();
    verify(notifications).announcement("inst-1", List.of(5L, 6L), 1L, "Docker", 10L, "Examen");

    StepVerifier.create(useCase.announce(ANA, COURSE, "x", "y", false)).expectError(ForbiddenException.class).verify();
    StepVerifier.create(useCase.announce(TEACHER, COURSE, " ", "y", false)).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void aFailedNotificationDoesNotUndoTheAnnouncement() {
    when(notifications.announcement(any(), any(), any(), any(), any(), any())).thenReturn(Mono.error(new RuntimeException("db")));
    StepVerifier.create(useCase.announce(TEACHER, COURSE, "Examen", "El viernes", false)).expectNextCount(1).verifyComplete();
  }

  @Test
  void studentThreadsNotifyStaffAndRepliesNotifyTheAuthor() {
    StepVerifier.create(useCase.openThread(ANA, COURSE, "Duda", "¿Cómo?")).expectNextCount(1).verifyComplete();
    verify(notifications).forumThread("inst-1", 1L, "Docker", 20L, "Duda");

    StepVerifier.create(useCase.reply(LUIS, COURSE, 20L, "Así")).expectNextCount(1).verifyComplete();
    verify(notifications).forumReply("inst-1", 5L, 1L, "Docker", 20L, "Duda");

    StepVerifier.create(useCase.reply(ANA, COURSE, 20L, "Gracias")).expectNextCount(1).verifyComplete();
    verify(notifications, times(1)).forumReply(any(), any(), any(), any(), any(), any());
  }

  @Test
  void lockedThreadsOnlyTakeStaffReplies() {
    when(forum.findThread(20L)).thenReturn(Mono.just(thread(true)));
    StepVerifier.create(useCase.reply(LUIS, COURSE, 20L, "Hola")).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.reply(TEACHER, COURSE, 20L, "Cerrado")).expectNextCount(1).verifyComplete();
  }

  @Test
  void onlyAuthorsEditAndOnlyStaffModerate() {
    StepVerifier.create(useCase.updateThread(LUIS, 1L, 20L, "Otro", "x", null, null)).expectError(ForbiddenException.class).verify();
    StepVerifier.create(useCase.updateThread(ANA, 1L, 20L, "Duda", "¿Cómo?", true, null)).expectError(ForbiddenException.class).verify();
    StepVerifier.create(useCase.updateThread(TEACHER, 1L, 20L, "Otro", "x", null, null)).expectError(ForbiddenException.class).verify();

    StepVerifier.create(useCase.updateThread(TEACHER, 1L, 20L, "Duda", "¿Cómo?", true, true))
      .expectNextMatches(t -> t.pinned() && t.locked()).verifyComplete();
    StepVerifier.create(useCase.updateThread(ANA, 1L, 20L, "Duda editada", "¿Cómo?", null, null))
      .expectNextMatches(t -> t.title().equals("Duda editada")).verifyComplete();
  }

  @Test
  void studentsDeleteOnlyTheirOwnRepliesAndThreadsOfOtherCoursesAreHidden() {
    ForumPost ana = new ForumPost(30L, 20L, 5L, "Ana Ruiz", "STUDENT", "x", T, T);
    when(forum.findPost(30L)).thenReturn(Mono.just(ana));
    when(forum.deletePost(any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.deletePost(LUIS, 1L, 30L)).expectError(ForbiddenException.class).verify();
    StepVerifier.create(useCase.deletePost(TEACHER, 1L, 30L)).verifyComplete();
    StepVerifier.create(useCase.deletePost(ANA, 2L, 30L)).expectError(IllegalArgumentException.class).verify();
    verify(forum, times(1)).deletePost(ana);
  }

  private static ForumThread thread(boolean locked) {
    return new ForumThread(20L, 1L, 5L, "Ana Ruiz", "STUDENT", "Duda", "¿Cómo?", false, locked, 0, T, T, T);
  }

  private static ForumThread withId(ForumThread t, Long id) {
    return new ForumThread(id, t.courseId(), t.authorId(), t.authorName(), t.authorRole(), t.title(), t.body(),
      t.pinned(), t.locked(), t.replyCount(), t.createdAt(), t.updatedAt(), t.lastActivityAt());
  }

  private static Enrollment enrollment(Long studentId) {
    return new Enrollment(studentId * 10, studentId, 1L, "active", T, 0, null);
  }
}
