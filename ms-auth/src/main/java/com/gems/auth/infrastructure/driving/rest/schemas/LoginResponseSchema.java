package com.gems.auth.infrastructure.driving.rest.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Response object containing login details and token")
public class LoginResponseSchema {
    @Schema(description = "Unique identifier of the user", example = "1")
    public Long userId;
    
    @Schema(description = "User's full name", example = "John Doe")
    public String name;
    
    @Schema(description = "User's email address", example = "john.doe@example.com")
    public String email;
    
    @Schema(description = "User's role", example = "STUDENT")
    public String role;
    
    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJIUzI1NiJ9...")
    public String token;
}
