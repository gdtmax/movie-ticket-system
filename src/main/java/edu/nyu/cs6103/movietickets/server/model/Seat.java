package edu.nyu.cs6103.movietickets.server.model;

import java.util.Locale;

public record Seat(
        long id,
        long theaterId,
        String rowLabel,
        int seatNumber,
        boolean accessible) {

    public Seat {
        requireNonNegativeId(id, "id");
        requirePositiveId(theaterId, "theaterId");
        if (rowLabel == null || rowLabel.isBlank()) {
            throw new IllegalArgumentException("rowLabel must not be blank");
        }
        rowLabel = rowLabel.trim().toUpperCase(Locale.ROOT);
        if (seatNumber <= 0) {
            throw new IllegalArgumentException("seatNumber must be greater than zero");
        }
    }

    public String displayName() {
        return rowLabel + seatNumber;
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
