package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.Actor;
import com.gems.education.application.CourseCommunityUseCase;
import com.gems.education.application.CourseCommunityUseCase.Course;
import com.gems.education.application.CourseCommunityUseCase.ThreadDetail;
import com.gems.education.application.gateway.UserDirectory;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.domain.entities.Announcement;
import com.gems.education.domain.entities.ForumPost;
import com.gems.education.domain.entities.ForumThread;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import com.gems.shared.web.Paging;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Announcements and forum of a course, for the staff of its institution and its enrolled
 * students. Students only see published courses (as with the rest of the course).
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}")
public class CourseCommunityController {
  private final CourseCommunityUseCase community;
  private final EducationAccess access;
  private final UserDirectory users;

  public CourseCommunityController(CourseCommunityUseCase community, EducationAccess access, UserDirectory users) {
    this.community = community;
    this.access = access;
    this.users = users;
  }

  // ── Announcements ──────────────────────────────────────────────────────────

  @GetMapping("/announcements")
  public Mono<ResponseEntity<List<Announcement>>> announcements(@PathVariable Long courseId) {
    return participant(courseId).then(Mono.defer(() -> community.announcements(courseId).collectList()))
      .map(ResponseEntity::ok);
  }

  @PostMapping("/announcements")
  public Mono<ResponseEntity<Announcement>> announce(@PathVariable Long courseId, @RequestBody AnnouncementRequest body) {
    return writer(courseId).flatMap(w -> community.announce(w.actor(), w.course(), body.title(), body.body(),
        Boolean.TRUE.equals(body.pinned())))
      .map(a -> ResponseEntity.status(HttpStatus.CREATED).body(a));
  }

  @PutMapping("/announcements/{id}")
  public Mono<ResponseEntity<Announcement>> updateAnnouncement(@PathVariable Long courseId, @PathVariable Long id,
                                                               @RequestBody AnnouncementRequest body) {
    return writer(courseId).flatMap(w -> community.updateAnnouncement(w.actor(), courseId, id, body.title(), body.body(),
        Boolean.TRUE.equals(body.pinned())))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/announcements/{id}")
  public Mono<ResponseEntity<Void>> deleteAnnouncement(@PathVariable Long courseId, @PathVariable Long id) {
    return writer(courseId).flatMap(w -> community.deleteAnnouncement(w.actor(), courseId, id))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  // ── Forum ──────────────────────────────────────────────────────────────────

  @GetMapping("/forum/threads")
  public Mono<ResponseEntity<List<ForumThread>>> threads(@PathVariable Long courseId,
                                                         @RequestParam(required = false) String search,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int limit) {
    int size = Math.max(1, Math.min(limit, Paging.MAX_LIMIT));
    return participant(courseId).then(Mono.defer(() -> community.threads(courseId, search, Math.max(0, page), size)))
      .map(p -> ResponseEntity.ok().header(Paging.TOTAL_HEADER, String.valueOf(p.total())).body(p.items()));
  }

  @GetMapping("/forum/threads/{threadId}")
  public Mono<ResponseEntity<ThreadDetail>> thread(@PathVariable Long courseId, @PathVariable Long threadId) {
    return participant(courseId).then(Mono.defer(() -> community.thread(courseId, threadId))).map(ResponseEntity::ok);
  }

  @PostMapping("/forum/threads")
  public Mono<ResponseEntity<ForumThread>> openThread(@PathVariable Long courseId, @RequestBody ThreadRequest body) {
    return writer(courseId).flatMap(w -> community.openThread(w.actor(), w.course(), body.title(), body.body()))
      .map(t -> ResponseEntity.status(HttpStatus.CREATED).body(t));
  }

  @PutMapping("/forum/threads/{threadId}")
  public Mono<ResponseEntity<ForumThread>> updateThread(@PathVariable Long courseId, @PathVariable Long threadId,
                                                        @RequestBody ThreadRequest body) {
    return writer(courseId).flatMap(w -> community.updateThread(w.actor(), courseId, threadId, body.title(), body.body(),
        body.pinned(), body.locked()))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/forum/threads/{threadId}")
  public Mono<ResponseEntity<Void>> deleteThread(@PathVariable Long courseId, @PathVariable Long threadId) {
    return writer(courseId).flatMap(w -> community.deleteThread(w.actor(), courseId, threadId))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  @PostMapping("/forum/threads/{threadId}/posts")
  public Mono<ResponseEntity<ForumPost>> reply(@PathVariable Long courseId, @PathVariable Long threadId,
                                               @RequestBody PostRequest body) {
    return writer(courseId).flatMap(w -> community.reply(w.actor(), w.course(), threadId, body.body()))
      .map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p));
  }

  @PutMapping("/forum/posts/{postId}")
  public Mono<ResponseEntity<ForumPost>> updatePost(@PathVariable Long courseId, @PathVariable Long postId,
                                                    @RequestBody PostRequest body) {
    return writer(courseId).flatMap(w -> community.updatePost(w.actor(), courseId, postId, body.body()))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/forum/posts/{postId}")
  public Mono<ResponseEntity<Void>> deletePost(@PathVariable Long courseId, @PathVariable Long postId) {
    return writer(courseId).flatMap(w -> community.deletePost(w.actor(), courseId, postId))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  // ── Access ─────────────────────────────────────────────────────────────────

  /** The course if the caller may read it and, being a student, is enrolled. */
  private Mono<Participation> participant(Long courseId) {
    return access.readableCourse(courseId).flatMap(course -> CurrentUser.get().flatMap(caller -> {
      if (caller.isStaff()) return Mono.just(new Participation(caller, course));
      return community.enrolled(caller.userId(), courseId).flatMap(enrolled -> enrolled
        ? Mono.just(new Participation(caller, course))
        : Mono.error(new ForbiddenException("Enroll in the course to take part")));
    }));
  }

  /** A participant with the display name their messages carry. */
  private Mono<Writer> writer(Long courseId) {
    return participant(courseId).flatMap(p -> users.find(p.caller().userId())
      .map(UserDirectory.UserProfile::fullName)
      .filter(name -> !name.isBlank())
      .onErrorResume(e -> Mono.empty())
      .defaultIfEmpty("Usuario")
      .map(name -> new Writer(new Actor(p.caller().userId(), name, p.caller().role(), p.caller().isStaff()),
        new Course(p.course().getId(), p.course().getInstitutionId(), p.course().getTitle()))));
  }

  private record Participation(AuthenticatedUser caller, CourseResponse course) {
  }

  private record Writer(Actor actor, Course course) {
  }

  public record AnnouncementRequest(String title, String body, Boolean pinned) {
  }

  /** pinned and locked are for the course staff; null leaves them unchanged. */
  public record ThreadRequest(String title, String body, Boolean pinned, Boolean locked) {
  }

  public record PostRequest(String body) {
  }
}
