package edu.nyu.cs6103.movietickets.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeDto(
        long id,
        long movieId,
        long theaterId,
        LocalDateTime startTime,
        BigDecimal price,
        String status) {
}
