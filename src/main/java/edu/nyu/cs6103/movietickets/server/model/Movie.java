package edu.nyu.cs6103.movietickets.server.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record Movie(
        long id,
        String title,
        int durationMinutes,
        String description,
        String genre,
        String posterPath,
        boolean active,
        LocalDateTime createdAt) {

    public Movie {
        requireNonNegativeId(id, "id");
        title = requireNonBlank(title, "title");
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than zero");
        }
        description = description == null ? "" : description.trim();
        genre = genre == null ? "" : genre.trim();
        posterPath = normalizeOptional(posterPath);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
