package com.gems.admin.infrastructure.driving.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request object for creating or updating an institution")
public class InstitutionRequest {

  @Schema(description = "Institution unique ID", example = "inst-001")
  @NotBlank(message = "Institution ID is required")
  private String id;

  @Schema(description = "Institution name", example = "Gems University")
  @NotBlank(message = "Institution name is required")
  private String name;

  @Schema(description = "Institution type", example = "UNIVERSITY")
  @NotBlank(message = "Institution type is required")
  private String type;

  @Schema(description = "Institution status", example = "ACTIVE")
  @NotBlank(message = "Institution status is required")
  private String status;

  @Schema(description = "Institution metadata details")
  @NotNull(message = "Institution metadata is required")
  private InstitutionMetadataRequest metadata;

  public InstitutionRequest() {
  }

  public InstitutionRequest(String id, String name, String type, String status, InstitutionMetadataRequest metadata) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.status = status;
    this.metadata = metadata;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public InstitutionMetadataRequest getMetadata() {
    return metadata;
  }

  public void setMetadata(InstitutionMetadataRequest metadata) {
    this.metadata = metadata;
  }
}
