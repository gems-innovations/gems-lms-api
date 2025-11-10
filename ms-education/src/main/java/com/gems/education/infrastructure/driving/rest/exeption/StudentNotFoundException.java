package com.gems.education.infrastructure.driving.rest.exeption;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String message) {
        super(message);
    }
}