package com.gems.auth.application.command;

public record RegisterUserCommand(String name, String email, String password, String role, String institutionId) {}
