package com.gems.admin.infrastructure.driving.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request object for institution metadata")
public class InstitutionMetadataRequest {

  @Schema(description = "Description of the institution", example = "An educational institution")
  private String description;

  @Schema(description = "Website URL", example = "https://www.example.edu")
  private String website;

  @Schema(description = "Contact Email", example = "contact@example.edu")
  @NotBlank(message = "Contact email is required")
  private String contactEmail;

  @Schema(description = "Phone Number", example = "+1-555-0199")
  private String phoneNumber;

  @Schema(description = "Address", example = "123 Education Way, Boston, MA")
  private String address;

  @Schema(description = "Subscription Type", example = "PREMIUM")
  @NotBlank(message = "Subscription type is required")
  private String subscriptionType;

  @Schema(description = "Max Users limit", example = "500")
  @NotNull(message = "Max users limit is required")
  private Integer maxUsers;

  public InstitutionMetadataRequest() {
  }

  public InstitutionMetadataRequest(String description, String website, String contactEmail, String phoneNumber,
                                    String address, String subscriptionType, Integer maxUsers) {
    this.description = description;
    this.website = website;
    this.contactEmail = contactEmail;
    this.phoneNumber = phoneNumber;
    this.address = address;
    this.subscriptionType = subscriptionType;
    this.maxUsers = maxUsers;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getSubscriptionType() {
    return subscriptionType;
  }

  public void setSubscriptionType(String subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public Integer getMaxUsers() {
    return maxUsers;
  }

  public void setMaxUsers(Integer maxUsers) {
    this.maxUsers = maxUsers;
  }
}
