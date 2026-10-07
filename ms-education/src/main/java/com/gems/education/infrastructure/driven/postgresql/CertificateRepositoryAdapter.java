package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CertificateGateway;
import com.gems.education.domain.entities.Certificate;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public class CertificateRepositoryAdapter implements CertificateGateway {
  private static final String COLUMNS = "id, code, student_id, student_name, institution_id, resource_type, "
    + "resource_id, resource_title, instructor_name, completed_at, issued_at, revoked_at";
  private final DatabaseClient db;

  public CertificateRepositoryAdapter(DatabaseClient db) { this.db = db; }

  @Override
  public Mono<Certificate> save(Certificate c) {
    var spec = db.sql("INSERT INTO certificates(code, student_id, student_name, institution_id, resource_type, "
        + "resource_id, resource_title, instructor_name, completed_at, issued_at) VALUES "
        + "(:code,:student,:name,:institution,:type,:resource,:title,:instructor,:completed,:issued) "
        + "ON CONFLICT(student_id, resource_type, resource_id) DO UPDATE SET code=certificates.code RETURNING " + COLUMNS)
      .bind("code", c.code()).bind("student", c.studentId()).bind("name", c.studentName())
      .bind("institution", c.institutionId()).bind("type", c.resourceType()).bind("resource", c.resourceId())
      .bind("title", c.resourceTitle()).bind("completed", c.completedAt()).bind("issued", c.issuedAt());
    spec = c.instructorName() == null ? spec.bindNull("instructor", String.class)
      : spec.bind("instructor", c.instructorName());
    return spec.map((row, meta) -> map(row)).one();
  }

  @Override
  public Mono<Certificate> find(Long studentId, String type, Long resourceId) {
    return db.sql("SELECT " + COLUMNS + " FROM certificates WHERE student_id=:student AND resource_type=:type AND resource_id=:resource")
      .bind("student", studentId).bind("type", type).bind("resource", resourceId)
      .map((row, meta) -> map(row)).one();
  }

  @Override
  public Mono<Certificate> findByCode(String code) {
    return db.sql("SELECT " + COLUMNS + " FROM certificates WHERE code=:code")
      .bind("code", code).map((row, meta) -> map(row)).one();
  }

  @Override
  public Flux<Certificate> findByStudent(Long studentId) {
    return db.sql("SELECT " + COLUMNS + " FROM certificates WHERE student_id=:student ORDER BY issued_at DESC")
      .bind("student", studentId).map((row, meta) -> map(row)).all();
  }

  private static Certificate map(Row row) {
    return new Certificate(row.get("id", Long.class), row.get("code", String.class),
      row.get("student_id", Long.class), row.get("student_name", String.class),
      row.get("institution_id", String.class), row.get("resource_type", String.class),
      row.get("resource_id", Long.class), row.get("resource_title", String.class),
      row.get("instructor_name", String.class), row.get("completed_at", LocalDateTime.class),
      row.get("issued_at", LocalDateTime.class), row.get("revoked_at", LocalDateTime.class));
  }
}
