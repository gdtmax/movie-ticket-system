package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.BookingDao;
import edu.nyu.cs6103.movietickets.server.dao.BookingSeatDao;
import edu.nyu.cs6103.movietickets.server.dao.SeatDao;
import edu.nyu.cs6103.movietickets.server.dao.ShowtimeDao;
import edu.nyu.cs6103.movietickets.server.dao.UserDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseInitializer;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.db.TransactionManager;
import edu.nyu.cs6103.movietickets.server.exception.AuthorizationException;
import edu.nyu.cs6103.movietickets.server.exception.SeatAlreadyBookedException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingServiceTest {

    @TempDir
    Path tempDirectory;

    private BookingService bookingService;
    private SeatService seatService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager manager = new DatabaseManager(
                "jdbc:sqlite:" + tempDirectory.resolve("bookings.db"));
        new DatabaseInitializer(manager).initialize(
                testResource("test-schema.sql"), testResource("test-seed.sql"));

        BookingSeatDao bookingSeatDao = new BookingSeatDao();
        ShowtimeDao showtimeDao = new ShowtimeDao();
        SeatDao seatDao = new SeatDao();
        Clock clock = Clock.fixed(Instant.parse("2029-01-01T00:00:00Z"), ZoneOffset.UTC);

        bookingService = new BookingService(
                manager, new TransactionManager(manager), new UserDao(), showtimeDao,
                seatDao, new BookingDao(), bookingSeatDao, clock);
        seatService = new SeatService(manager, showtimeDao, seatDao, bookingSeatDao);
    }

    @Test
    void createsAnAtomicMultiSeatBookingAndHistory() {
        BookingService.BookingDetails details =
                bookingService.createBooking(2, 1, List.of(1L, 2L));

        assertEquals(new BigDecimal("20.00"), details.booking().totalPrice());
        assertEquals(2, details.seats().size());
        assertEquals(2, bookingService.getBookingHistory(2).getFirst().seats().size());
        assertEquals(2, seatService.getSeatMap(1).bookedCount());
    }

    @Test
    void rollsBackTheWholeOrderIfAnyRequestedSeatConflicts() {
        bookingService.createBooking(1, 1, List.of(1L));

        assertThrows(SeatAlreadyBookedException.class,
                () -> bookingService.createBooking(2, 1, List.of(2L, 1L)));

        assertTrue(bookingService.getBookingHistory(2).isEmpty());
        SeatService.SeatMap map = seatService.getSeatMap(1);
        assertEquals(1, map.bookedCount());
        assertTrue(map.seats().stream()
                .filter(seat -> seat.seat().id() == 2)
                .findFirst().orElseThrow().available());
    }

    @Test
    void cancellationKeepsHistoryAndReleasesSeats() {
        BookingService.BookingDetails created =
                bookingService.createBooking(1, 1, List.of(1L));
        BookingService.BookingDetails cancelled =
                bookingService.cancelBooking(1, created.booking().id());

        assertEquals(BookingStatus.CANCELLED, cancelled.booking().status());
        assertFalse(cancelled.seats().getFirst().isActive());
        assertEquals(0, seatService.getSeatMap(1).bookedCount());

        BookingService.BookingDetails replacement =
                bookingService.createBooking(2, 1, List.of(1L));
        assertEquals(BookingStatus.CONFIRMED, replacement.booking().status());
    }

    @Test
    void validatesSeatOwnershipAndBookingOwnership() {
        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(2, 1, List.of()));
        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(2, 1, List.of(4L)));

        BookingService.BookingDetails booking =
                bookingService.createBooking(1, 1, List.of(1L));
        assertThrows(AuthorizationException.class,
                () -> bookingService.cancelBooking(2, booking.booking().id()));
    }

    private static Path testResource(String name) throws Exception {
        return Path.of(BookingServiceTest.class.getClassLoader().getResource(name).toURI());
    }
}
