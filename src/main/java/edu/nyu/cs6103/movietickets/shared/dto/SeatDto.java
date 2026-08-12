package edu.nyu.cs6103.movietickets.shared.dto;

public record SeatDto(
        long id,
        String rowLabel,
        int seatNumber,
        boolean accessible,
        String status) {
}
