package com.gems.education.application;

import com.gems.education.application.gateway.AnnouncementGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.ForumGateway;
import com.gems.education.domain.entities.Announcement;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.ForumPost;
import com.gems.education.domain.entities.ForumThread;
import com.gems.shared.security.ForbiddenException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Announcements and forum of a course. Callers are already known to take part in the course
 * (staff of its institution or enrolled students). Staff publish announcements and moderate the
 * forum (pin, lock, delete anything); everyone edits and deletes their own messages.
 */
public class CourseCommunityUseCase {
  public static final int MAX_TITLE = 200;
  public static final int MAX_BODY = 20_000;

  private final AnnouncementGateway announcements;
  private final ForumGateway forum;
  private final EnrollmentGateway enrollments;
  private final NotificationUseCase notifications;

  public CourseCommunityUseCase(AnnouncementGateway announcements, ForumGateway forum, EnrollmentGateway enrollments,
                                NotificationUseCase notifications) {
    this.announcements = announcements;
    this.forum = forum;
    this.enrollments = enrollments;
    this.notifications = notifications;
  }

  /** Students take part only in the courses they are enrolled in. */
  public Mono<Boolean> enrolled(Long studentId, Long courseId) {
    return enrollments.existsByStudentIdAndCourseId(studentId, courseId);
  }

  // ── Announcements ──────────────────────────────────────────────────────────

  public Flux<Announcement> announcements(Long courseId) {
    return announcements.findByCourse(courseId);
  }

  /** Publishes and notifies every student enrolled in the course (a failed notice does not undo it). */
  public Mono<Announcement> announce(Actor actor, Course course, String title, String body, boolean pinned) {
    return requireStaff(actor).then(validate(title, body)).then(Mono.defer(() -> {
      LocalDateTime now = LocalDateTime.now();
      return announcements.save(new Announcement(null, course.id(), actor.userId(), actor.name(), title.trim(),
        body.trim(), pinned, now, now));
    })).flatMap(saved -> enrollments.findByCourseId(course.id()).map(Enrollment::getStudentId).collectList()
      .flatMap(students -> notifications.announcement(course.institutionId(), students, course.id(), course.title(),
        saved.id(), saved.title()))
      .onErrorResume(e -> Mono.empty())
      .thenReturn(saved));
  }

  public Mono<Announcement> updateAnnouncement(Actor actor, Long courseId, Long id, String title, String body,
                                               boolean pinned) {
    return requireStaff(actor).then(validate(title, body)).then(announcement(courseId, id))
      .flatMap(a -> announcements.save(new Announcement(a.id(), a.courseId(), a.authorId(), a.authorName(),
        title.trim(), body.trim(), pinned, a.createdAt(), LocalDateTime.now())));
  }

  public Mono<Void> deleteAnnouncement(Actor actor, Long courseId, Long id) {
    return requireStaff(actor).then(announcement(courseId, id)).flatMap(a -> announcements.delete(a.id()));
  }

  // ── Forum ──────────────────────────────────────────────────────────────────

  public Mono<ThreadPage> threads(Long courseId, String search, int page, int limit) {
    String q = search == null || search.isBlank() ? null : search.trim();
    return Mono.zip(forum.threads(courseId, q, page * limit, limit).collectList(), forum.countThreads(courseId, q))
      .map(t -> new ThreadPage(t.getT1(), t.getT2()));
  }

  public Mono<ThreadDetail> thread(Long courseId, Long threadId) {
    return threadOf(courseId, threadId)
      .flatMap(t -> forum.posts(t.id()).collectList().map(posts -> new ThreadDetail(t, posts)));
  }

  /** Opens a thread; the course staff are notified of questions from students. */
  public Mono<ForumThread> openThread(Actor actor, Course course, String title, String body) {
    return validate(title, body).then(Mono.defer(() -> {
      LocalDateTime now = LocalDateTime.now();
      return forum.saveThread(new ForumThread(null, course.id(), actor.userId(), actor.name(), actor.role(),
        title.trim(), body.trim(), false, false, 0, now, now, now));
    })).flatMap(saved -> (actor.staff() ? Mono.<Void>empty()
        : notifications.forumThread(course.institutionId(), course.id(), course.title(), saved.id(), saved.title()).then())
      .onErrorResume(e -> Mono.empty())
      .thenReturn(saved));
  }

  /** Authors edit title and body; staff also pin and lock (null leaves them as they are). */
  public Mono<ForumThread> updateThread(Actor actor, Long courseId, Long threadId, String title, String body,
                                        Boolean pinned, Boolean locked) {
    return validate(title, body).then(threadOf(courseId, threadId)).flatMap(t -> {
      boolean own = Objects.equals(t.authorId(), actor.userId());
      boolean moderating = (pinned != null && pinned != t.pinned()) || (locked != null && locked != t.locked());
      if (!own && !actor.staff()) return Mono.error(new ForbiddenException("You can only edit your own messages"));
      if (moderating && !actor.staff()) return Mono.error(new ForbiddenException("Only the course staff moderate the forum"));
      boolean edited = !t.title().equals(title.trim()) || !t.body().equals(body.trim());
      if (edited && !own) return Mono.error(new ForbiddenException("You can only edit your own messages"));
      return forum.saveThread(new ForumThread(t.id(), t.courseId(), t.authorId(), t.authorName(), t.authorRole(),
        title.trim(), body.trim(), pinned != null ? pinned : t.pinned(), locked != null ? locked : t.locked(),
        t.replyCount(), t.createdAt(), edited ? LocalDateTime.now() : t.updatedAt(), t.lastActivityAt()));
    });
  }

  public Mono<Void> deleteThread(Actor actor, Long courseId, Long threadId) {
    return threadOf(courseId, threadId)
      .flatMap(t -> canChange(actor, t.authorId()).then(Mono.defer(() -> forum.deleteThread(t.id()))));
  }

  /** Replies; locked threads only take replies from staff. The thread author is notified. */
  public Mono<ForumPost> reply(Actor actor, Course course, Long threadId, String body) {
    return validate("-", body).then(threadOf(course.id(), threadId)).flatMap(t -> {
      if (t.locked() && !actor.staff()) return Mono.error(new IllegalArgumentException("This thread is closed"));
      LocalDateTime now = LocalDateTime.now();
      return forum.savePost(new ForumPost(null, t.id(), actor.userId(), actor.name(), actor.role(), body.trim(), now, now))
        .flatMap(saved -> (Objects.equals(t.authorId(), actor.userId()) ? Mono.<Void>empty()
            : notifications.forumReply(course.institutionId(), t.authorId(), course.id(), course.title(), t.id(), t.title()).then())
          .onErrorResume(e -> Mono.empty())
          .thenReturn(saved));
    });
  }

  public Mono<ForumPost> updatePost(Actor actor, Long courseId, Long postId, String body) {
    return validate("-", body).then(post(courseId, postId)).flatMap(p -> {
      if (!Objects.equals(p.authorId(), actor.userId())) {
        return Mono.error(new ForbiddenException("You can only edit your own messages"));
      }
      return forum.savePost(new ForumPost(p.id(), p.threadId(), p.authorId(), p.authorName(), p.authorRole(),
        body.trim(), p.createdAt(), LocalDateTime.now()));
    });
  }

  public Mono<Void> deletePost(Actor actor, Long courseId, Long postId) {
    return post(courseId, postId).flatMap(p -> canChange(actor, p.authorId()).then(Mono.defer(() -> forum.deletePost(p))));
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private Mono<Announcement> announcement(Long courseId, Long id) {
    return announcements.findById(id).filter(a -> Objects.equals(a.courseId(), courseId))
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Announcement not found")));
  }

  private Mono<ForumThread> threadOf(Long courseId, Long id) {
    return forum.findThread(id).filter(t -> Objects.equals(t.courseId(), courseId))
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Thread not found")));
  }

  /** The reply, if its thread belongs to the course. */
  private Mono<ForumPost> post(Long courseId, Long id) {
    return forum.findPost(id)
      .flatMap(p -> forum.findThread(p.threadId()).filter(t -> Objects.equals(t.courseId(), courseId)).map(t -> p))
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Message not found")));
  }

  private static Mono<Void> canChange(Actor actor, Long authorId) {
    return actor.staff() || Objects.equals(authorId, actor.userId()) ? Mono.empty()
      : Mono.error(new ForbiddenException("You can only delete your own messages"));
  }

  private static Mono<Void> requireStaff(Actor actor) {
    return actor.staff() ? Mono.empty() : Mono.error(new ForbiddenException("Only the course staff publish announcements"));
  }

  private static Mono<Void> validate(String title, String body) {
    if (title == null || title.isBlank() || title.trim().length() > MAX_TITLE) {
      return Mono.error(new IllegalArgumentException("The title is required (max " + MAX_TITLE + " characters)"));
    }
    if (body == null || body.isBlank() || body.length() > MAX_BODY) {
      return Mono.error(new IllegalArgumentException("The message is required (max " + MAX_BODY + " characters)"));
    }
    return Mono.empty();
  }

  /** The course being written in: what notifications need. */
  public record Course(Long id, String institutionId, String title) {
  }

  public record ThreadPage(List<ForumThread> items, long total) {
  }

  public record ThreadDetail(ForumThread thread, List<ForumPost> posts) {
  }
}
