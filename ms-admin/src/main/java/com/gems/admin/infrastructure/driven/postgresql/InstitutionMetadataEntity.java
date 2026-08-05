package com.gems.admin.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("institution_metadata")
public class InstitutionMetadataEntity implements Persistable<String> {
  @Id
  @Column("institution_id")
  private String institutionId;

  private String description;
  private String website;

  @Column("contact_email")
  private String contactEmail;

  @Column("phone_number")
  private String phoneNumber;

  private String address;

  @Column("subscription_type")
  private String subscriptionType;

  @Column("max_users")
  private Integer maxUsers;

  @Column("last_activity")
  private LocalDateTime lastActivity;

  @Transient
  private boolean isNew = true;

  public InstitutionMetadataEntity() {
  }

  public InstitutionMetadataEntity(String institutionId, String description, String website, String contactEmail,
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

  @Override
  public String getId() {
    return institutionId;
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

  @Override
  public boolean isNew() {
    return isNew;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }
}
