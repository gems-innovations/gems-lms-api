package com.gems.admin.infrastructure.driving.rest.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "InstitutionResponse", description = "Response object containing institution details")
public class InstitutionResponseSchema {
  @Schema(description = "Institution unique ID", example = "inst-001")
  public String id;

  @Schema(description = "Institution name", example = "Gems University")
  public String name;

  @Schema(description = "Institution type", example = "UNIVERSITY")
  public String type;

  @Schema(description = "Institution status", example = "ACTIVE")
  public String status;

  @Schema(description = "Number of active users in the institution", example = "42")
  public Integer usersCount;

  @Schema(description = "Timestamp when the institution was created", example = "2026-06-18T10:00:00")
  public LocalDateTime createdAt;

  @Schema(description = "Timestamp when the institution was last updated", example = "2026-06-18T10:00:00")
  public LocalDateTime updatedAt;

  @Schema(description = "Institution metadata details")
  public InstitutionMetadataResponseSchema metadata;
}
