package com.gems.admin.infrastructure.driven.postgresql;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("institution_home_versions")
public record HomeVersionEntity(
  @Id Long id,
  @Column("institution_id") String institutionId,
  @Column("version") Integer version,
  @Column("mode") String mode,
  @Column("html") String html,
  @Column("css") String css,
  @Column("note") String note,
  @Column("published_by") Long publishedBy,
  @Column("published_at") LocalDateTime publishedAt) {
}
