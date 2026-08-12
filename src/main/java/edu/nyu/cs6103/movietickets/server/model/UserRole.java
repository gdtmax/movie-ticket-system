package edu.nyu.cs6103.movietickets.server.model;

import java.util.Locale;

public enum UserRole {
    USER,
    ADMIN;

    public static UserRole fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User role must not be blank");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown user role: " + value, exception);
        }
    }

    public String toDatabaseValue() {
        return name();
    }
}
