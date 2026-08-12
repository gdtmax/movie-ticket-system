package edu.nyu.cs6103.movietickets.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

public record Showtime(
        long id,
        long movieId,
        long theaterId,
        LocalDateTime startTime,
        BigDecimal price,
        Status status,
        LocalDateTime createdAt) {

    public Showtime {
        requireNonNegativeId(id, "id");
        requirePositiveId(movieId, "movieId");
        requirePositiveId(theaterId, "theaterId");
        startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        price = requireMoney(price, "price");
        status = Objects.requireNonNull(status, "status must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isScheduled() {
        return status == Status.SCHEDULED;
    }

    public enum Status {
        SCHEDULED,
        CANCELLED;

        public static Status fromDatabaseValue(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Showtime status must not be blank");
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown showtime status: " + value, exception);
            }
        }

        public String toDatabaseValue() {
            return name();
        }
    }

    private static BigDecimal requireMoney(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(name + " must have at most two decimal places");
        }
        return value.setScale(2);
    }

    private static void requireNonNegativeId(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static void requirePositiveId(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}
