package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.application.gateway.AuditGateway;
import com.gems.auth.domain.entities.AuditEvent;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditRepositoryAdapter implements AuditGateway {
  private static final String SELECT = """
    SELECT id, actor_user_id, actor_role, institution_id, action, http_method, resource_path,
           response_status, client_ip, user_agent, occurred_at FROM audit_events
    """;
  private final DatabaseClient db;

  public AuditRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<AuditEvent> save(AuditEvent event) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("""
      INSERT INTO audit_events(actor_user_id, actor_role, institution_id, action, http_method,
        resource_path, response_status, client_ip, user_agent, occurred_at)
      VALUES (:actor, :role, :institution, :action, :method, :path, :status, :ip, :agent, :time)
      RETURNING id, actor_user_id, actor_role, institution_id, action, http_method, resource_path,
        response_status, client_ip, user_agent, occurred_at
      """)
      .bind("actor", event.actorUserId()).bind("role", event.actorRole())
      .bind("action", event.action()).bind("method", event.httpMethod())
      .bind("path", event.resourcePath()).bind("status", event.responseStatus())
      .bind("time", event.occurredAt());
    spec = bindNullable(spec, "institution", event.institutionId(), String.class);
    spec = bindNullable(spec, "ip", event.clientIp(), String.class);
    spec = bindNullable(spec, "agent", event.userAgent(), String.class);
    return spec.map((row, metadata) -> map(row)).one();
  }

  @Override
  public Mono<Page> search(String institutionId, String search, String action, LocalDateTime from,
                           LocalDateTime to, int limit, long offset) {
    List<String> clauses = new ArrayList<>();
    if (institutionId != null) clauses.add("institution_id = :institution");
    if (search != null && !search.isBlank()) clauses.add("(resource_path ILIKE :search OR CAST(actor_user_id AS TEXT) ILIKE :search)");
    if (action != null && !action.isBlank()) clauses.add("action = :action");
    if (from != null) clauses.add("occurred_at >= :from");
    if (to != null) clauses.add("occurred_at < :to");
    String where = clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    String pageSql = SELECT + where + " ORDER BY occurred_at DESC LIMIT :limit OFFSET :offset";
    DatabaseClient.GenericExecuteSpec page = bind(db.sql(pageSql), institutionId, search, action, from, to)
      .bind("limit", limit).bind("offset", offset);
    DatabaseClient.GenericExecuteSpec count = bind(db.sql("SELECT COUNT(*) AS total FROM audit_events" + where),
      institutionId, search, action, from, to);
    return Mono.zip(page.map((row, metadata) -> map(row)).all().collectList(),
      count.map((row, metadata) -> row.get("total", Long.class)).one())
      .map(data -> new Page(data.getT1(), data.getT2()));
  }

  private static DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec,
      String institutionId, String search, String action, LocalDateTime from, LocalDateTime to) {
    if (institutionId != null) spec = spec.bind("institution", institutionId);
    if (search != null && !search.isBlank()) spec = spec.bind("search", "%" + search.trim() + "%");
    if (action != null && !action.isBlank()) spec = spec.bind("action", action);
    if (from != null) spec = spec.bind("from", from);
    if (to != null) spec = spec.bind("to", to);
    return spec;
  }

  private static <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec,
      String name, T value, Class<T> type) {
    return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
  }

  private static AuditEvent map(io.r2dbc.spi.Row row) {
    Integer status = row.get("response_status", Integer.class);
    return new AuditEvent(row.get("id", Long.class), row.get("actor_user_id", Long.class),
      row.get("actor_role", String.class), row.get("institution_id", String.class),
      row.get("action", String.class), row.get("http_method", String.class),
      row.get("resource_path", String.class), status == null ? 0 : status,
      row.get("client_ip", String.class), row.get("user_agent", String.class),
      row.get("occurred_at", LocalDateTime.class));
  }
}
