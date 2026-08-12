package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.BookingDao;
import edu.nyu.cs6103.movietickets.server.dao.BookingSeatDao;
import edu.nyu.cs6103.movietickets.server.dao.SeatDao;
import edu.nyu.cs6103.movietickets.server.dao.ShowtimeDao;
import edu.nyu.cs6103.movietickets.server.dao.UserDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.db.TransactionManager;
import edu.nyu.cs6103.movietickets.server.exception.AuthorizationException;
import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.SeatAlreadyBookedException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.Booking;
import edu.nyu.cs6103.movietickets.server.model.BookingSeat;
import edu.nyu.cs6103.movietickets.server.model.BookingStatus;
import edu.nyu.cs6103.movietickets.server.model.Seat;
import edu.nyu.cs6103.movietickets.server.model.Showtime;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Coordinates atomic multi-seat booking, history, and cancellation operations. */
public final class BookingService {

    private static final int MAX_BUSY_RETRIES = 25;
    private static final long MAX_RETRY_DELAY_MILLIS = 100;

    private final DatabaseManager databaseManager;
    private final TransactionManager transactionManager;
    private final UserDao userDao;
    private final ShowtimeDao showtimeDao;
    private final SeatDao seatDao;
    private final BookingDao bookingDao;
    private final BookingSeatDao bookingSeatDao;
    private final Clock clock;

    public BookingService(
            DatabaseManager databaseManager,
            TransactionManager transactionManager,
            UserDao userDao,
            ShowtimeDao showtimeDao,
            SeatDao seatDao,
            BookingDao bookingDao,
            BookingSeatDao bookingSeatDao) {
        this(databaseManager, transactionManager, userDao, showtimeDao, seatDao,
                bookingDao, bookingSeatDao, Clock.systemUTC());
    }

    public BookingService(
            DatabaseManager databaseManager,
            TransactionManager transactionManager,
            UserDao userDao,
            ShowtimeDao showtimeDao,
            SeatDao seatDao,
            BookingDao bookingDao,
            BookingSeatDao bookingSeatDao,
            Clock clock) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
        this.showtimeDao = Objects.requireNonNull(showtimeDao, "showtimeDao must not be null");
        this.seatDao = Objects.requireNonNull(seatDao, "seatDao must not be null");
        this.bookingDao = Objects.requireNonNull(bookingDao, "bookingDao must not be null");
        this.bookingSeatDao = Objects.requireNonNull(bookingSeatDao, "bookingSeatDao must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public BookingDetails createBooking(long userId, long showtimeId, List<Long> seatIds) {
        requirePositiveId(userId, "userId");
        requirePositiveId(showtimeId, "showtimeId");
        List<Long> requestedSeatIds = validateSeatIds(seatIds);

        return executeWriteTransaction(connection -> {
                requireUser(connection, userId);
                Showtime showtime = requireBookableShowtime(connection, showtimeId);
                Map<Long, Seat> theaterSeats = loadTheaterSeats(connection, showtime.theaterId());
                for (long seatId : requestedSeatIds) {
                    if (!theaterSeats.containsKey(seatId)) {
                        throw new ValidationException(
                                "Seat " + seatId + " does not belong to the showtime theater");
                    }
                }

                BigDecimal totalPrice = showtime.price()
                        .multiply(BigDecimal.valueOf(requestedSeatIds.size()));
                Booking booking = bookingDao.insert(connection, userId, showtimeId, totalPrice);
                List<BookingSeat> bookingSeats = new ArrayList<>();

                for (long seatId : requestedSeatIds) {
                    try {
                        bookingSeats.add(bookingSeatDao.insert(
                                connection, booking.id(), showtimeId, seatId, showtime.price()));
                    } catch (DatabaseOperationException exception) {
                        if (isSeatConstraint(exception)) {
                            throw new SeatAlreadyBookedException(showtimeId, seatId);
                        }
                        throw exception;
                    }
                }
                return new BookingDetails(booking, bookingSeats);
        });
    }

    public BookingDetails getBooking(long userId, long bookingId) {
        requirePositiveId(userId, "userId");
        requirePositiveId(bookingId, "bookingId");
        try (Connection connection = databaseManager.getConnection()) {
            requireUser(connection, userId);
            Booking booking = requireOwnedBooking(connection, userId, bookingId);
            return details(connection, booking);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load booking", exception);
        }
    }

    public List<BookingDetails> getBookingHistory(long userId) {
        requirePositiveId(userId, "userId");
        try (Connection connection = databaseManager.getConnection()) {
            requireUser(connection, userId);
            List<BookingDetails> history = new ArrayList<>();
            for (Booking booking : bookingDao.findByUserId(connection, userId)) {
                history.add(details(connection, booking));
            }
            return List.copyOf(history);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load booking history", exception);
        }
    }

    public BookingDetails cancelBooking(long userId, long bookingId) {
        requirePositiveId(userId, "userId");
        requirePositiveId(bookingId, "bookingId");

        return executeWriteTransaction(connection -> {
                requireUser(connection, userId);
                Booking booking = requireOwnedBooking(connection, userId, bookingId);
                if (booking.status() == BookingStatus.CANCELLED) {
                    throw new ValidationException("Booking is already cancelled");
                }

                Showtime showtime = showtimeDao.findById(connection, booking.showtimeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Showtime", booking.showtimeId()));
                LocalDateTime now = LocalDateTime.now(clock);
                if (!showtime.startTime().isAfter(now)) {
                    throw new ValidationException(
                            "A booking cannot be cancelled after the showtime has started");
                }

                int releasedSeats = bookingSeatDao.cancelByBookingId(connection, bookingId);
                if (releasedSeats == 0) {
                    throw new DatabaseOperationException(
                            "Confirmed booking has no active seats to release");
                }
                if (!bookingDao.cancel(connection, bookingId, now)) {
                    throw new DatabaseOperationException("Booking status changed during cancellation");
                }

                Booking cancelled = bookingDao.findById(connection, bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
                return details(connection, cancelled);
        });
    }

    private void requireUser(Connection connection, long userId) {
        if (userDao.findById(connection, userId).isEmpty()) {
            throw new ResourceNotFoundException("User", userId);
        }
    }

    private <T> T executeWriteTransaction(TransactionManager.TransactionWork<T> work) {
        for (int attempt = 1; attempt <= MAX_BUSY_RETRIES; attempt++) {
            try {
                return transactionManager.execute(work);
            } catch (SQLException exception) {
                if (!isBusy(exception) || attempt == MAX_BUSY_RETRIES) {
                    throw new DatabaseOperationException("Unable to execute booking transaction", exception);
                }
            } catch (DatabaseOperationException exception) {
                if (!isBusy(exception) || attempt == MAX_BUSY_RETRIES) {
                    throw exception;
                }
            }
            waitBeforeRetry(attempt);
        }
        throw new IllegalStateException("Unreachable retry state");
    }

    private static void waitBeforeRetry(int attempt) {
        long upperBound = Math.min(MAX_RETRY_DELAY_MILLIS, 10L * attempt);
        long delay = ThreadLocalRandom.current().nextLong(5, upperBound + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DatabaseOperationException(
                    "Interrupted while waiting to retry a locked database", exception);
        }
    }

    private static boolean isBusy(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && (message.contains("SQLITE_BUSY")
                    || message.toLowerCase().contains("database is locked"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Showtime requireBookableShowtime(Connection connection, long showtimeId) {
        Showtime showtime = showtimeDao.findById(connection, showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", showtimeId));
        if (!showtime.isScheduled()) {
            throw new ValidationException("The showtime is cancelled");
        }
        if (!showtime.startTime().isAfter(LocalDateTime.now(clock))) {
            throw new ValidationException("The showtime has already started");
        }
        return showtime;
    }

    private Booking requireOwnedBooking(Connection connection, long userId, long bookingId) {
        Booking booking = bookingDao.findById(connection, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (booking.userId() != userId) {
            throw new AuthorizationException("The booking belongs to another user");
        }
        return booking;
    }

    private Map<Long, Seat> loadTheaterSeats(Connection connection, long theaterId) {
        Map<Long, Seat> seatsById = new HashMap<>();
        for (Seat seat : seatDao.findByTheaterId(connection, theaterId)) {
            seatsById.put(seat.id(), seat);
        }
        return seatsById;
    }

    private BookingDetails details(Connection connection, Booking booking) {
        return new BookingDetails(
                booking, bookingSeatDao.findByBookingId(connection, booking.id()));
    }

    private static List<Long> validateSeatIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ValidationException("At least one seat must be selected");
        }
        List<Long> copy = new ArrayList<>(seatIds);
        Set<Long> unique = new HashSet<>();
        for (Long seatId : copy) {
            if (seatId == null || seatId <= 0) {
                throw new ValidationException("Every seatId must be greater than zero");
            }
            if (!unique.add(seatId)) {
                throw new ValidationException("A seat cannot appear more than once in a booking");
            }
        }
        return List.copyOf(copy);
    }

    private static boolean isSeatConstraint(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.contains("booking_seats.showtime_id")
                    && message.contains("booking_seats.seat_id")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void requirePositiveId(long value, String name) {
        if (value <= 0) {
            throw new ValidationException(name + " must be greater than zero");
        }
    }

    public record BookingDetails(Booking booking, List<BookingSeat> seats) {
        public BookingDetails {
            Objects.requireNonNull(booking, "booking must not be null");
            seats = List.copyOf(Objects.requireNonNull(seats, "seats must not be null"));
        }
    }
}
