package edu.nyu.cs6103.movietickets.server.model;

import java.util.Locale;

public enum BookingStatus {
    CONFIRMED,
    CANCELLED;

    public static BookingStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Booking status must not be blank");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown booking status: " + value, exception);
        }
    }

    public String toDatabaseValue() {
        return name();
    }
}
