package edu.nyu.cs6103.movietickets.server.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record User(
        long id,
        String username,
        String passwordHash,
        UserRole role,
        LocalDateTime createdAt) {

    public User {
        requireNonNegativeId(id, "id");
        username = requireNonBlank(username, "username");
        passwordHash = requireNonBlank(passwordHash, "passwordHash");
        role = Objects.requireNonNull(role, "role must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static void requireNonNegativeId(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
