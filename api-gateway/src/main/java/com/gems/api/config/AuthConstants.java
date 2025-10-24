package com.gems.api.config;

public final class AuthConstants {
    
    private AuthConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String X_USER_ID_HEADER = "X-User-Id";
    public static final String X_USER_ROLE_HEADER = "X-User-Role";
}
