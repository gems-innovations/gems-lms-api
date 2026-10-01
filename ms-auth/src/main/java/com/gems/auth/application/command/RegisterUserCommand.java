package com.gems.auth.application.command;

public record RegisterUserCommand(String firstName, String lastName, String username, String email,
                                   String password, String role, String institutionId) {}
