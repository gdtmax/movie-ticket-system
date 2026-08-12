package edu.nyu.cs6103.movietickets.server.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 6, 12, 0);

    @Test
    void normalizesValuesAndProvidesDomainHelpers() {
        User admin = new User(1, " admin ", " hash ", UserRole.ADMIN, CREATED_AT);
        Seat seat = new Seat(1, 1, " a ", 8, false);

        assertEquals("admin", admin.username());
        assertTrue(admin.isAdmin());
        assertEquals("A8", seat.displayName());
        assertEquals(UserRole.ADMIN, UserRole.fromDatabaseValue("admin"));
        assertEquals(BookingStatus.CANCELLED,
                BookingStatus.fromDatabaseValue("cancelled"));
    }

    @Test
    void modelsUseBigDecimalForMoney() {
        Showtime showtime = new Showtime(
                1, 1, 1, CREATED_AT.plusDays(1), new BigDecimal("15.00"),
                Showtime.Status.SCHEDULED, CREATED_AT);
        BookingSeat bookingSeat = new BookingSeat(
                1, 1, 1, BookingStatus.CONFIRMED, new BigDecimal("15.00"));

        assertEquals(new BigDecimal("15.00"), showtime.price());
        assertTrue(showtime.isScheduled());
        assertTrue(bookingSeat.isActive());
    }

    @Test
    void rejectsInvalidMoneyAndIdentifiers() {
        assertThrows(IllegalArgumentException.class, () ->
                new Seat(1, 0, "A", 1, false));
        assertThrows(IllegalArgumentException.class, () ->
                new Showtime(1, 1, 1, CREATED_AT, new BigDecimal("1.999"),
                        Showtime.Status.SCHEDULED, CREATED_AT));
    }

    @Test
    void bookingStatusAndCancellationTimeMustAgree() {
        Booking confirmed = new Booking(
                1, 1, 1, BookingStatus.CONFIRMED,
                new BigDecimal("15.00"), CREATED_AT, null);
        assertFalse(confirmed.isCancelled());

        assertThrows(IllegalArgumentException.class, () ->
                new Booking(1, 1, 1, BookingStatus.CANCELLED,
                        new BigDecimal("15.00"), CREATED_AT, null));
        assertThrows(IllegalArgumentException.class, () ->
                new Booking(1, 1, 1, BookingStatus.CONFIRMED,
                        new BigDecimal("15.00"), CREATED_AT, CREATED_AT.plusMinutes(1)));
    }
}
