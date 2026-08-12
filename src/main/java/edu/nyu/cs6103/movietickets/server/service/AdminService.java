package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.*;
import edu.nyu.cs6103.movietickets.server.db.TransactionManager;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.*;
import edu.nyu.cs6103.movietickets.shared.dto.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

/** Transactional administrator write operations. Authorization remains in the router. */
public final class AdminService {
    private final TransactionManager transactions;
    private final MovieDao movies;
    private final TheaterDao theaters;
    private final SeatDao seats;
    private final ShowtimeDao showtimes;

    public AdminService(TransactionManager transactions, MovieDao movies,
                        TheaterDao theaters, SeatDao seats, ShowtimeDao showtimes) {
        this.transactions = Objects.requireNonNull(transactions);
        this.movies = Objects.requireNonNull(movies);
        this.theaters = Objects.requireNonNull(theaters);
        this.seats = Objects.requireNonNull(seats);
        this.showtimes = Objects.requireNonNull(showtimes);
    }

    public Movie createMovie(AdminMovieRequest request) throws SQLException {
        validateMovie(request);
        return transactions.execute(c -> movies.insert(c, new Movie(0, request.title(),
                request.durationMinutes(), request.description(), request.genre(),
                request.posterPath(), request.active(), LocalDateTime.now())));
    }

    public Movie updateMovie(AdminMovieRequest request) throws SQLException {
        validateMovie(request);
        if (request.movieId() <= 0) throw new ValidationException("movieId must be positive");
        return transactions.execute(c -> {
            Movie old = movies.findById(c, request.movieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Movie", request.movieId()));
            Movie updated = new Movie(old.id(), request.title(), request.durationMinutes(),
                    request.description(), request.genre(), request.posterPath(), request.active(), old.createdAt());
            if (!movies.update(c, updated)) throw new ResourceNotFoundException("Movie", request.movieId());
            return updated;
        });
    }

    public Theater createTheater(AdminTheaterRequest request) throws SQLException {
        validateTheater(request, true);
        return transactions.execute(c -> {
            Theater theater = theaters.insert(c, request.name().trim(), request.location().trim());
            for (int row = 0; row < request.rowCount(); row++) {
                String label = String.valueOf((char) ('A' + row));
                for (int number = 1; number <= request.seatsPerRow(); number++) {
                    seats.insert(c, theater.id(), label, number, false);
                }
            }
            return theater;
        });
    }

    public Theater updateTheater(AdminTheaterRequest request) throws SQLException {
        validateTheater(request, false);
        return transactions.execute(c -> {
            Theater old = theaters.findById(c, request.theaterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Theater", request.theaterId()));
            Theater updated = new Theater(old.id(), request.name(), request.location(), old.createdAt());
            if (!theaters.update(c, updated)) throw new ResourceNotFoundException("Theater", request.theaterId());
            return updated;
        });
    }

    public Showtime createShowtime(AdminShowtimeRequest request) throws SQLException {
        validateShowtime(request);
        return transactions.execute(c -> {
            requireReferences(c, request);
            return showtimes.insert(c, request.movieId(), request.theaterId(), request.startTime(), request.price());
        });
    }

    public Showtime updateShowtime(AdminShowtimeRequest request) throws SQLException {
        validateShowtime(request);
        if (request.showtimeId() <= 0) throw new ValidationException("showtimeId must be positive");
        return transactions.execute(c -> {
            requireReferences(c, request);
            Showtime old = showtimes.findById(c, request.showtimeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Showtime", request.showtimeId()));
            Showtime updated = new Showtime(old.id(), request.movieId(), request.theaterId(),
                    request.startTime(), request.price(), Showtime.Status.fromDatabaseValue(request.status()), old.createdAt());
            if (!showtimes.update(c, updated)) throw new ResourceNotFoundException("Showtime", request.showtimeId());
            return updated;
        });
    }

    private void requireReferences(java.sql.Connection c, AdminShowtimeRequest r) {
        if (movies.findById(c, r.movieId()).isEmpty()) throw new ResourceNotFoundException("Movie", r.movieId());
        if (theaters.findById(c, r.theaterId()).isEmpty()) throw new ResourceNotFoundException("Theater", r.theaterId());
    }
    private static void validateMovie(AdminMovieRequest r) {
        Objects.requireNonNull(r); if (r.title() == null || r.title().isBlank() || r.durationMinutes() <= 0)
            throw new ValidationException("Movie title and positive duration are required");
    }
    private static void validateTheater(AdminTheaterRequest r, boolean creating) {
        Objects.requireNonNull(r); if (r.name() == null || r.name().isBlank() || r.location() == null || r.location().isBlank())
            throw new ValidationException("Theater name and location are required");
        if (creating && (r.rowCount() < 1 || r.rowCount() > 26 || r.seatsPerRow() < 1 || r.seatsPerRow() > 100))
            throw new ValidationException("Rows must be 1-26 and seats per row must be 1-100");
    }
    private static void validateShowtime(AdminShowtimeRequest r) {
        Objects.requireNonNull(r); if (r.movieId() <= 0 || r.theaterId() <= 0 || r.startTime() == null || r.price() == null || r.price().signum() < 0)
            throw new ValidationException("Movie, theater, start time, and nonnegative price are required");
    }
}
