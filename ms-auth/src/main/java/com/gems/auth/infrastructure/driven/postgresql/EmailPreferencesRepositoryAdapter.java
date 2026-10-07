package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.application.EmailPreferences;
import com.gems.auth.application.gateway.EmailPreferencesGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class EmailPreferencesRepositoryAdapter implements EmailPreferencesGateway {
  private final DatabaseClient db;

  public EmailPreferencesRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<EmailPreferences> find(Long userId) {
    return db.sql("SELECT course_notices, tips FROM email_preferences WHERE user_id = :userId")
      .bind("userId", userId)
      .map(row -> new EmailPreferences(Boolean.TRUE.equals(row.get("course_notices", Boolean.class)),
        Boolean.TRUE.equals(row.get("tips", Boolean.class))))
      .one()
      .defaultIfEmpty(EmailPreferences.DEFAULTS);
  }

  @Override
  public Mono<Void> save(Long userId, EmailPreferences preferences) {
    return db.sql("INSERT INTO email_preferences (user_id, course_notices, tips, updated_at) "
        + "VALUES (:userId, :notices, :tips, CURRENT_TIMESTAMP) "
        + "ON CONFLICT (user_id) DO UPDATE SET course_notices = EXCLUDED.course_notices, "
        + "tips = EXCLUDED.tips, updated_at = CURRENT_TIMESTAMP")
      .bind("userId", userId).bind("notices", preferences.courseNotices()).bind("tips", preferences.tips())
      .then();
  }
}
