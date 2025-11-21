package com.gems.auth.application.response;

public record LoginResponse(Long userId, String name, String email, String role, String token) {}
