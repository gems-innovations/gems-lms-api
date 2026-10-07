package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.AnnouncementGateway;
import com.gems.education.application.gateway.ForumGateway;
import com.gems.education.domain.entities.Announcement;
import com.gems.education.domain.entities.ForumPost;
import com.gems.education.domain.entities.ForumThread;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** Announcements and forum of the courses. */
@Repository
public class CourseCommunityRepositoryAdapter implements AnnouncementGateway, ForumGateway {
  private static final String ANNOUNCEMENT = "id, course_id, author_id, author_name, title, body, pinned, created_at, updated_at";
  private static final String THREAD = "id, course_id, author_id, author_name, author_role, title, body, pinned, locked, "
    + "reply_count, created_at, updated_at, last_activity_at";
  private static final String POST = "id, thread_id, author_id, author_name, author_role, body, created_at, updated_at";
  private static final String THREAD_FILTER = "course_id = :course AND (CAST(:search AS VARCHAR) IS NULL "
    + "OR title ILIKE '%' || :search || '%' OR body ILIKE '%' || :search || '%')";

  private final DatabaseClient db;
  private final TransactionalOperator tx;

  public CourseCommunityRepositoryAdapter(DatabaseClient db, TransactionalOperator tx) {
    this.db = db;
    this.tx = tx;
  }

  // ── Announcements ──────────────────────────────────────────────────────────

  @Override
  public Flux<Announcement> findByCourse(Long courseId) {
    return db.sql("SELECT " + ANNOUNCEMENT + " FROM course_announcements WHERE course_id = :course "
        + "ORDER BY pinned DESC, created_at DESC")
      .bind("course", courseId).map((row, meta) -> announcement(row)).all();
  }

  @Override
  public Mono<Announcement> findById(Long id) {
    return db.sql("SELECT " + ANNOUNCEMENT + " FROM course_announcements WHERE id = :id").bind("id", id)
      .map((row, meta) -> announcement(row)).one();
  }

  @Override
  public Mono<Announcement> save(Announcement a) {
    DatabaseClient.GenericExecuteSpec spec = a.id() == null
      ? db.sql("INSERT INTO course_announcements(course_id, author_id, author_name, title, body, pinned, created_at, updated_at) "
          + "VALUES (:course, :author, :authorName, :title, :body, :pinned, :created, :updated) RETURNING " + ANNOUNCEMENT)
          .bind("course", a.courseId()).bind("author", a.authorId()).bind("authorName", a.authorName())
          .bind("created", a.createdAt())
      : db.sql("UPDATE course_announcements SET title = :title, body = :body, pinned = :pinned, updated_at = :updated "
          + "WHERE id = :id RETURNING " + ANNOUNCEMENT).bind("id", a.id());
    return spec.bind("title", a.title()).bind("body", a.body()).bind("pinned", a.pinned()).bind("updated", a.updatedAt())
      .map((row, meta) -> announcement(row)).one();
  }

  @Override
  public Mono<Void> delete(Long id) {
    return db.sql("DELETE FROM course_announcements WHERE id = :id").bind("id", id).then();
  }

  // ── Forum ──────────────────────────────────────────────────────────────────

  @Override
  public Flux<ForumThread> threads(Long courseId, String search, int offset, int limit) {
    return search(db.sql("SELECT " + THREAD + " FROM forum_threads WHERE " + THREAD_FILTER
        + " ORDER BY pinned DESC, last_activity_at DESC, id DESC LIMIT :limit OFFSET :offset"), courseId, search)
      .bind("limit", limit).bind("offset", offset)
      .map((row, meta) -> thread(row)).all();
  }

  @Override
  public Mono<Long> countThreads(Long courseId, String search) {
    return search(db.sql("SELECT COUNT(*) AS total FROM forum_threads WHERE " + THREAD_FILTER), courseId, search)
      .map((row, meta) -> row.get("total", Long.class)).one();
  }

  @Override
  public Mono<ForumThread> findThread(Long id) {
    return db.sql("SELECT " + THREAD + " FROM forum_threads WHERE id = :id").bind("id", id)
      .map((row, meta) -> thread(row)).one();
  }

  @Override
  public Mono<ForumThread> saveThread(ForumThread t) {
    DatabaseClient.GenericExecuteSpec spec = t.id() == null
      ? db.sql("INSERT INTO forum_threads(course_id, author_id, author_name, author_role, title, body, pinned, locked, "
          + "created_at, updated_at, last_activity_at) VALUES (:course, :author, :authorName, :authorRole, :title, :body, "
          + ":pinned, :locked, :created, :updated, :activity) RETURNING " + THREAD)
          .bind("course", t.courseId()).bind("author", t.authorId()).bind("authorName", t.authorName())
          .bind("authorRole", t.authorRole()).bind("created", t.createdAt()).bind("activity", t.lastActivityAt())
      : db.sql("UPDATE forum_threads SET title = :title, body = :body, pinned = :pinned, locked = :locked, "
          + "updated_at = :updated WHERE id = :id RETURNING " + THREAD).bind("id", t.id());
    return spec.bind("title", t.title()).bind("body", t.body()).bind("pinned", t.pinned()).bind("locked", t.locked())
      .bind("updated", t.updatedAt())
      .map((row, meta) -> thread(row)).one();
  }

  @Override
  public Mono<Void> deleteThread(Long id) {
    return db.sql("DELETE FROM forum_threads WHERE id = :id").bind("id", id).then();
  }

  @Override
  public Flux<ForumPost> posts(Long threadId) {
    return db.sql("SELECT " + POST + " FROM forum_posts WHERE thread_id = :thread ORDER BY created_at, id")
      .bind("thread", threadId).map((row, meta) -> post(row)).all();
  }

  @Override
  public Mono<ForumPost> findPost(Long id) {
    return db.sql("SELECT " + POST + " FROM forum_posts WHERE id = :id").bind("id", id)
      .map((row, meta) -> post(row)).one();
  }

  @Override
  public Mono<ForumPost> savePost(ForumPost p) {
    if (p.id() != null) {
      return db.sql("UPDATE forum_posts SET body = :body, updated_at = :updated WHERE id = :id RETURNING " + POST)
        .bind("id", p.id()).bind("body", p.body()).bind("updated", p.updatedAt())
        .map((row, meta) -> post(row)).one();
    }
    return db.sql("INSERT INTO forum_posts(thread_id, author_id, author_name, author_role, body, created_at, updated_at) "
        + "VALUES (:thread, :author, :authorName, :authorRole, :body, :created, :created) RETURNING " + POST)
      .bind("thread", p.threadId()).bind("author", p.authorId()).bind("authorName", p.authorName())
      .bind("authorRole", p.authorRole()).bind("body", p.body()).bind("created", p.createdAt())
      .map((row, meta) -> post(row)).one()
      .flatMap(saved -> db.sql("UPDATE forum_threads SET reply_count = reply_count + 1, last_activity_at = :at WHERE id = :thread")
        .bind("at", saved.createdAt()).bind("thread", saved.threadId()).then().thenReturn(saved))
      .as(tx::transactional);
  }

  @Override
  public Mono<Void> deletePost(ForumPost p) {
    return db.sql("DELETE FROM forum_posts WHERE id = :id").bind("id", p.id()).then()
      .then(db.sql("UPDATE forum_threads SET reply_count = GREATEST(reply_count - 1, 0) WHERE id = :thread")
        .bind("thread", p.threadId()).then())
      .as(tx::transactional);
  }

  private static DatabaseClient.GenericExecuteSpec search(DatabaseClient.GenericExecuteSpec spec, Long courseId, String search) {
    spec = spec.bind("course", courseId);
    return search == null ? spec.bindNull("search", String.class) : spec.bind("search", search);
  }

  private static Announcement announcement(Row r) {
    return new Announcement(r.get("id", Long.class), r.get("course_id", Long.class), r.get("author_id", Long.class),
      r.get("author_name", String.class), r.get("title", String.class), r.get("body", String.class),
      Boolean.TRUE.equals(r.get("pinned", Boolean.class)), r.get("created_at", LocalDateTime.class),
      r.get("updated_at", LocalDateTime.class));
  }

  private static ForumThread thread(Row r) {
    return new ForumThread(r.get("id", Long.class), r.get("course_id", Long.class), r.get("author_id", Long.class),
      r.get("author_name", String.class), r.get("author_role", String.class), r.get("title", String.class),
      r.get("body", String.class), Boolean.TRUE.equals(r.get("pinned", Boolean.class)),
      Boolean.TRUE.equals(r.get("locked", Boolean.class)), r.get("reply_count", Integer.class),
      r.get("created_at", LocalDateTime.class), r.get("updated_at", LocalDateTime.class),
      r.get("last_activity_at", LocalDateTime.class));
  }

  private static ForumPost post(Row r) {
    return new ForumPost(r.get("id", Long.class), r.get("thread_id", Long.class), r.get("author_id", Long.class),
      r.get("author_name", String.class), r.get("author_role", String.class), r.get("body", String.class),
      r.get("created_at", LocalDateTime.class), r.get("updated_at", LocalDateTime.class));
  }
}
