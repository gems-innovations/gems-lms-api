package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.AcademicPeriodGateway;
import com.gems.education.application.gateway.EnrollmentRulesGateway;
import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.EnrollmentRules;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Academic periods and course enrollment rules. */
@Repository
public class EnrollmentRulesRepositoryAdapter implements EnrollmentRulesGateway, AcademicPeriodGateway {
  private static final String RULES = "course_id, period_id, opens_at, closes_at, capacity, self_enrollment, prerequisite_ids";
  private static final String PERIOD = "id, institution_id, name, starts_on, ends_on, created_at";

  private final DatabaseClient db;

  public EnrollmentRulesRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  // ── Rules ──────────────────────────────────────────────────────────────────

  @Override
  public Mono<EnrollmentRules> find(Long courseId) {
    return db.sql("SELECT " + RULES + " FROM course_enrollment_rules WHERE course_id = :course").bind("course", courseId)
      .map((row, meta) -> rules(row)).one();
  }

  @Override
  public Flux<EnrollmentRules> findAll(Collection<Long> courseIds) {
    if (courseIds.isEmpty()) return Flux.empty();
    return db.sql("SELECT " + RULES + " FROM course_enrollment_rules WHERE course_id = ANY(:ids)")
      .bind("ids", courseIds.toArray(Long[]::new)).map((row, meta) -> rules(row)).all();
  }

  @Override
  public Flux<EnrollmentRules> findByInstitution(String institutionId) {
    return db.sql("SELECT r.course_id, r.period_id, r.opens_at, r.closes_at, r.capacity, r.self_enrollment, r.prerequisite_ids "
        + "FROM course_enrollment_rules r JOIN courses c ON c.id = r.course_id WHERE c.institution_id = :institution")
      .bind("institution", institutionId).map((row, meta) -> rules(row)).all();
  }

  @Override
  public Mono<EnrollmentRules> save(EnrollmentRules r) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO course_enrollment_rules(" + RULES + ", updated_at) "
        + "VALUES (:course, :period, :opens, :closes, :capacity, :self, :prereqs, CURRENT_TIMESTAMP) "
        + "ON CONFLICT (course_id) DO UPDATE SET period_id = EXCLUDED.period_id, opens_at = EXCLUDED.opens_at, "
        + "closes_at = EXCLUDED.closes_at, capacity = EXCLUDED.capacity, self_enrollment = EXCLUDED.self_enrollment, "
        + "prerequisite_ids = EXCLUDED.prerequisite_ids, updated_at = CURRENT_TIMESTAMP RETURNING " + RULES)
      .bind("course", r.courseId()).bind("self", r.selfEnrollment())
      .bind("prereqs", r.prerequisiteIds().toArray(Long[]::new));
    spec = r.periodId() == null ? spec.bindNull("period", Long.class) : spec.bind("period", r.periodId());
    spec = r.opensAt() == null ? spec.bindNull("opens", LocalDateTime.class) : spec.bind("opens", r.opensAt());
    spec = r.closesAt() == null ? spec.bindNull("closes", LocalDateTime.class) : spec.bind("closes", r.closesAt());
    spec = r.capacity() == null ? spec.bindNull("capacity", Integer.class) : spec.bind("capacity", r.capacity());
    return spec.map((row, meta) -> rules(row)).one();
  }

  // ── Periods ────────────────────────────────────────────────────────────────

  @Override
  public Flux<AcademicPeriod> findPeriodsOf(String institutionId) {
    return db.sql("SELECT " + PERIOD + " FROM academic_periods WHERE institution_id = :institution ORDER BY starts_on DESC, id DESC")
      .bind("institution", institutionId).map((row, meta) -> period(row)).all();
  }

  @Override
  public Mono<AcademicPeriod> findById(Long id) {
    return db.sql("SELECT " + PERIOD + " FROM academic_periods WHERE id = :id").bind("id", id)
      .map((row, meta) -> period(row)).one();
  }

  @Override
  public Mono<AcademicPeriod> save(AcademicPeriod p) {
    DatabaseClient.GenericExecuteSpec spec = p.id() == null
      ? db.sql("INSERT INTO academic_periods(institution_id, name, starts_on, ends_on, created_at) "
          + "VALUES (:institution, :name, :starts, :ends, :created) RETURNING " + PERIOD)
          .bind("institution", p.institutionId()).bind("created", p.createdAt())
      : db.sql("UPDATE academic_periods SET name = :name, starts_on = :starts, ends_on = :ends WHERE id = :id RETURNING " + PERIOD)
          .bind("id", p.id());
    return spec.bind("name", p.name()).bind("starts", p.startsOn()).bind("ends", p.endsOn())
      .map((row, meta) -> period(row)).one();
  }

  @Override
  public Mono<Void> delete(Long id) {
    return db.sql("DELETE FROM academic_periods WHERE id = :id").bind("id", id).then();
  }

  private static EnrollmentRules rules(Row r) {
    Long[] prereqs = r.get("prerequisite_ids", Long[].class);
    List<Long> ids = prereqs == null ? List.of() : Arrays.asList(prereqs);
    return new EnrollmentRules(r.get("course_id", Long.class), r.get("period_id", Long.class),
      r.get("opens_at", LocalDateTime.class), r.get("closes_at", LocalDateTime.class), r.get("capacity", Integer.class),
      Boolean.TRUE.equals(r.get("self_enrollment", Boolean.class)), ids);
  }

  private static AcademicPeriod period(Row r) {
    return new AcademicPeriod(r.get("id", Long.class), r.get("institution_id", String.class), r.get("name", String.class),
      r.get("starts_on", LocalDate.class), r.get("ends_on", LocalDate.class), r.get("created_at", LocalDateTime.class));
  }
}
