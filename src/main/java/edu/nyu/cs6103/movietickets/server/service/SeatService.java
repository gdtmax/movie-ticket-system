package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.BookingSeatDao;
import edu.nyu.cs6103.movietickets.server.dao.SeatDao;
import edu.nyu.cs6103.movietickets.server.dao.ShowtimeDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.BookingSeat;
import edu.nyu.cs6103.movietickets.server.model.Seat;
import edu.nyu.cs6103.movietickets.server.model.Showtime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SeatService {

    private final DatabaseManager databaseManager;
    private final ShowtimeDao showtimeDao;
    private final SeatDao seatDao;
    private final BookingSeatDao bookingSeatDao;

    public SeatService(
            DatabaseManager databaseManager,
            ShowtimeDao showtimeDao,
            SeatDao seatDao,
            BookingSeatDao bookingSeatDao) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.showtimeDao = Objects.requireNonNull(showtimeDao, "showtimeDao must not be null");
        this.seatDao = Objects.requireNonNull(seatDao, "seatDao must not be null");
        this.bookingSeatDao = Objects.requireNonNull(bookingSeatDao, "bookingSeatDao must not be null");
    }

    public SeatMap getSeatMap(long showtimeId) {
        requirePositiveId(showtimeId, "showtimeId");
        try (Connection connection = databaseManager.getConnection()) {
            Showtime showtime = showtimeDao.findById(connection, showtimeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Showtime", showtimeId));
            if (!showtime.isScheduled()) {
                throw new ValidationException("Seat map is unavailable for a cancelled showtime");
            }

            List<Seat> theaterSeats = seatDao.findByTheaterId(connection, showtime.theaterId());
            Set<Long> bookedSeatIds = new HashSet<>();
            for (BookingSeat bookingSeat :
                    bookingSeatDao.findConfirmedByShowtimeId(connection, showtimeId)) {
                bookedSeatIds.add(bookingSeat.seatId());
            }

            List<SeatAvailability> availability = theaterSeats.stream()
                    .map(seat -> new SeatAvailability(
                            seat,
                            bookedSeatIds.contains(seat.id())
                                    ? SeatStatus.BOOKED
                                    : SeatStatus.AVAILABLE))
                    .toList();
            return new SeatMap(showtime, availability);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load seat map", exception);
        }
    }

    private static void requirePositiveId(long value, String name) {
        if (value <= 0) {
            throw new ValidationException(name + " must be greater than zero");
        }
    }

    public enum SeatStatus {
        AVAILABLE,
        BOOKED
    }

    public record SeatAvailability(Seat seat, SeatStatus status) {
        public SeatAvailability {
            Objects.requireNonNull(seat, "seat must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }

        public boolean available() {
            return status == SeatStatus.AVAILABLE;
        }
    }

    public record SeatMap(Showtime showtime, List<SeatAvailability> seats) {
        public SeatMap {
            Objects.requireNonNull(showtime, "showtime must not be null");
            seats = List.copyOf(Objects.requireNonNull(seats, "seats must not be null"));
        }

        public long availableCount() {
            return seats.stream().filter(SeatAvailability::available).count();
        }

        public long bookedCount() {
            return seats.size() - availableCount();
        }
    }
}
