package edu.nyu.cs6103.movietickets.server.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record Theater(
        long id,
        String name,
        String location,
        LocalDateTime createdAt) {

    public Theater {
        if (id < 0) {
            throw new IllegalArgumentException("id must not be negative");
        }
        name = requireNonBlank(name, "name");
        location = requireNonBlank(location, "location");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
