package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.NotificationGateway;
import com.gems.education.domain.entities.Notification;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Notifications and who has read them. A notification without recipient belongs to the staff
 * of its institution; each staff member reads it independently (notification_reads).
 */
@Repository
public class NotificationRepositoryAdapter implements NotificationGateway {
  /** Notifications visible to :userId (own) and, for staff, to their institution's staff. */
  private static final String VISIBLE = "(n.recipient_user_id = :userId "
    + "OR (:staff AND n.recipient_user_id IS NULL AND n.institution_id = :institutionId))";

  private final DatabaseClient db;

  public NotificationRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Notification> save(Notification n) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO notifications "
        + "(institution_id, recipient_user_id, type, title, message, course_id, reference_id, created_at) "
        + "VALUES (:institutionId, :recipient, :type, :title, :message, :courseId, :referenceId, :createdAt) "
        + "RETURNING *, false AS is_read")
      .bind("type", n.type()).bind("title", n.title()).bind("message", n.message())
      .bind("createdAt", n.createdAt());
    // Personal notices (enrolment, certificate, motivation) may have no institution.
    spec = n.institutionId() == null ? spec.bindNull("institutionId", String.class) : spec.bind("institutionId", n.institutionId());
    spec = n.recipientUserId() == null ? spec.bindNull("recipient", Long.class) : spec.bind("recipient", n.recipientUserId());
    spec = n.courseId() == null ? spec.bindNull("courseId", Long.class) : spec.bind("courseId", n.courseId());
    spec = n.referenceId() == null ? spec.bindNull("referenceId", Long.class) : spec.bind("referenceId", n.referenceId());
    return spec.map(NotificationRepositoryAdapter::toNotification).one();
  }

  @Override
  public Flux<Notification> findFor(Long userId, String institutionId, boolean staff, int limit) {
    return visible("SELECT n.*, (r.user_id IS NOT NULL) AS is_read FROM notifications n "
        + "LEFT JOIN notification_reads r ON r.notification_id = n.id AND r.user_id = :userId "
        + "WHERE " + VISIBLE + " ORDER BY n.created_at DESC LIMIT :limit", userId, institutionId, staff)
      .bind("limit", limit)
      .map(NotificationRepositoryAdapter::toNotification).all();
  }

  @Override
  public Mono<Notification> findById(Long id) {
    return db.sql("SELECT n.*, false AS is_read FROM notifications n WHERE n.id = :id")
      .bind("id", id)
      .map(NotificationRepositoryAdapter::toNotification).one();
  }

  @Override
  public Mono<Void> markRead(Long notificationId, Long userId) {
    return db.sql("INSERT INTO notification_reads (notification_id, user_id, read_at) VALUES (:id, :userId, :now) "
        + "ON CONFLICT DO NOTHING")
      .bind("id", notificationId).bind("userId", userId).bind("now", LocalDateTime.now())
      .then();
  }

  @Override
  public Mono<Void> markAllRead(Long userId, String institutionId, boolean staff) {
    return visible("INSERT INTO notification_reads (notification_id, user_id, read_at) "
        + "SELECT n.id, :userId, :now FROM notifications n WHERE " + VISIBLE + " ON CONFLICT DO NOTHING",
        userId, institutionId, staff)
      .bind("now", LocalDateTime.now())
      .then();
  }

  private DatabaseClient.GenericExecuteSpec visible(String sql, Long userId, String institutionId, boolean staff) {
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql).bind("userId", userId).bind("staff", staff);
    return institutionId == null ? spec.bindNull("institutionId", String.class) : spec.bind("institutionId", institutionId);
  }

  private static Notification toNotification(Readable row) {
    return new Notification(row.get("id", Long.class), row.get("institution_id", String.class),
      row.get("recipient_user_id", Long.class), row.get("type", String.class), row.get("title", String.class),
      row.get("message", String.class), row.get("course_id", Long.class), row.get("reference_id", Long.class),
      row.get("created_at", LocalDateTime.class), Boolean.TRUE.equals(row.get("is_read", Boolean.class)));
  }
}
