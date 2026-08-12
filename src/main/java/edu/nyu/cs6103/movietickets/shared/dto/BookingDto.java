package edu.nyu.cs6103.movietickets.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingDto(
        long id,
        long userId,
        long showtimeId,
        String status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        List<Long> seatIds) {

    public BookingDto {
        seatIds = List.copyOf(seatIds);
    }
}
