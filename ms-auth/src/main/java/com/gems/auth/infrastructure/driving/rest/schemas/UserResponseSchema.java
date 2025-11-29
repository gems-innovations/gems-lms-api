package com.gems.auth.infrastructure.driving.rest.schemas;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "UserResponse", description = "Response object containing user details")
public class UserResponseSchema {
    @Schema(description = "Unique identifier of the user", example = "1")
    public Long userId;
    
    @Schema(description = "User's full name", example = "John Doe")
    public String name;
    
    @Schema(description = "User's email address", example = "john.doe@example.com")
    public String email;
    
    @Schema(description = "User's role", example = "STUDENT")
    public String role;
    
    @Schema(description = "Timestamp when the user was created", example = "2023-11-25T10:00:00")
    public LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the user was last updated", example = "2023-11-25T10:00:00")
    public LocalDateTime updatedAt;
    
    @Schema(description = "Whether the user account is active", example = "true")
    public boolean active;
}
