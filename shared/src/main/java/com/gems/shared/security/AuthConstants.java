package com.gems.shared.security;

public final class AuthConstants {
    
    private AuthConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}

