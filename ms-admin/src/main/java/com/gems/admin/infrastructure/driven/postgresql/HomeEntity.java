package com.gems.admin.infrastructure.driven.postgresql;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("institution_home")
public record HomeEntity(
  @Id @Column("institution_id") String institutionId,
  @Column("slug") String slug,
  @Column("draft_mode") String draftMode,
  @Column("draft_html") String draftHtml,
  @Column("draft_css") String draftCss,
  @Column("draft_updated_at") LocalDateTime draftUpdatedAt,
  @Column("draft_updated_by") Long draftUpdatedBy,
  @Column("published_version") Integer publishedVersion) {
}
