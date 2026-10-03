package com.gems.auth.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("password_reset_tokens")
public class PasswordResetTokenEntity {
  @Id
  private Long id;
  @Column("user_id")
  private Long userId;
  @Column("token_hash")
  private String tokenHash;
  @Column("expires_at")
  private LocalDateTime expiresAt;
  @Column("used_at")
  private LocalDateTime usedAt;
  @Column("created_at")
  private LocalDateTime createdAt;

  public PasswordResetTokenEntity() {
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
  public LocalDateTime getUsedAt() { return usedAt; }
  public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
