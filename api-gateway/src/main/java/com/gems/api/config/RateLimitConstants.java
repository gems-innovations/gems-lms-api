package com.gems.api.config;

public final class RateLimitConstants {
    
    private RateLimitConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String X_REAL_IP_HEADER = "X-Real-IP";
    public static final String UNKNOWN_CLIENT = "unknown";
    
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    
    public static final String ZERO_REMAINING = "0";
    public static final long MILLISECONDS_PER_SECOND = 1000L;
}
