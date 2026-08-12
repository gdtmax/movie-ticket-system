package edu.nyu.cs6103.movietickets.shared.dto;

import java.util.List;

public record BookingResponse(BookingDto booking, List<BookingDto> bookings) {

    public BookingResponse {
        bookings = bookings == null ? List.of() : List.copyOf(bookings);
    }

    public static BookingResponse single(BookingDto booking) {
        return new BookingResponse(booking, List.of());
    }

    public static BookingResponse history(List<BookingDto> bookings) {
        return new BookingResponse(null, bookings);
    }
}
