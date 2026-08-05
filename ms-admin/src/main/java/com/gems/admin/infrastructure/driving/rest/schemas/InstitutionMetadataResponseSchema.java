package com.gems.admin.infrastructure.driving.rest.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "InstitutionMetadataResponse", description = "Response object containing institution metadata details")
public class InstitutionMetadataResponseSchema {
  @Schema(description = "Institution ID", example = "inst-001")
  public String institutionId;

  @Schema(description = "Description of the institution", example = "An educational institution")
  public String description;

  @Schema(description = "Website URL", example = "https://www.example.edu")
  public String website;

  @Schema(description = "Contact Email", example = "contact@example.edu")
  public String contactEmail;

  @Schema(description = "Phone Number", example = "+1-555-0199")
  public String phoneNumber;

  @Schema(description = "Address", example = "123 Education Way, Boston, MA")
  public String address;

  @Schema(description = "Subscription Type", example = "PREMIUM")
  public String subscriptionType;

  @Schema(description = "Max Users limit", example = "500")
  public Integer maxUsers;

  @Schema(description = "Timestamp of last activity", example = "2026-06-18T10:00:00")
  public LocalDateTime lastActivity;
}
