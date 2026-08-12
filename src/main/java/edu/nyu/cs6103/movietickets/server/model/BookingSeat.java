package edu.nyu.cs6103.movietickets.server.model;

import java.math.BigDecimal;
import java.util.Objects;

public record BookingSeat(
        long bookingId,
        long showtimeId,
        long seatId,
        BookingStatus status,
        BigDecimal priceAtBooking) {

    public BookingSeat {
        requirePositiveId(bookingId, "bookingId");
        requirePositiveId(showtimeId, "showtimeId");
        requirePositiveId(seatId, "seatId");
        status = Objects.requireNonNull(status, "status must not be null");
        priceAtBooking = requireMoney(priceAtBooking, "priceAtBooking");
    }

    public boolean isActive() {
        return status == BookingStatus.CONFIRMED;
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

    private static void requirePositiveId(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}
