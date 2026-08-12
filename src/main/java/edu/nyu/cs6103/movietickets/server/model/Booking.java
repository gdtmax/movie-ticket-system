package edu.nyu.cs6103.movietickets.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record Booking(
        long id,
        long userId,
        long showtimeId,
        BookingStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt) {

    public Booking {
        requireNonNegativeId(id, "id");
        requirePositiveId(userId, "userId");
        requirePositiveId(showtimeId, "showtimeId");
        status = Objects.requireNonNull(status, "status must not be null");
        totalPrice = requireMoney(totalPrice, "totalPrice");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");

        if (status == BookingStatus.CONFIRMED && cancelledAt != null) {
            throw new IllegalArgumentException("A confirmed booking cannot have cancelledAt");
        }
        if (status == BookingStatus.CANCELLED && cancelledAt == null) {
            throw new IllegalArgumentException("A cancelled booking must have cancelledAt");
        }
        if (cancelledAt != null && cancelledAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("cancelledAt cannot be before createdAt");
        }
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
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
