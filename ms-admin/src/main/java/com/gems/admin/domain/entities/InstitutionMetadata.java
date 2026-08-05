package com.gems.admin.domain.entities;

import java.time.LocalDateTime;

public class InstitutionMetadata {
  private String institutionId;
  private String description;
  private String website;
  private String contactEmail;
  private String phoneNumber;
  private String address;
  private String subscriptionType;
  private Integer maxUsers;
  private LocalDateTime lastActivity;

  public InstitutionMetadata() {
  }

  public InstitutionMetadata(String institutionId, String description, String website, String contactEmail,
                             String phoneNumber, String address, String subscriptionType, Integer maxUsers,
                             LocalDateTime lastActivity) {
    this.institutionId = institutionId;
    this.description = description;
    this.website = website;
    this.contactEmail = contactEmail;
    this.phoneNumber = phoneNumber;
    this.address = address;
    this.subscriptionType = subscriptionType;
    this.maxUsers = maxUsers;
    this.lastActivity = lastActivity;
  }

  public String getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(String institutionId) {
    this.institutionId = institutionId;
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

  public LocalDateTime getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(LocalDateTime lastActivity) {
    this.lastActivity = lastActivity;
  }
}
