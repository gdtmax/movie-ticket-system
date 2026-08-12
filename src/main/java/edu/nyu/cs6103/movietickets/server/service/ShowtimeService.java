package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.MovieDao;
import edu.nyu.cs6103.movietickets.server.dao.ShowtimeDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.Showtime;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class ShowtimeService {

    private final DatabaseManager databaseManager;
    private final ShowtimeDao showtimeDao;
    private final MovieDao movieDao;
    private final Clock clock;

    public ShowtimeService(
            DatabaseManager databaseManager, ShowtimeDao showtimeDao, MovieDao movieDao) {
        this(databaseManager, showtimeDao, movieDao, Clock.systemUTC());
    }

    public ShowtimeService(
            DatabaseManager databaseManager,
            ShowtimeDao showtimeDao,
            MovieDao movieDao,
            Clock clock) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.showtimeDao = Objects.requireNonNull(showtimeDao, "showtimeDao must not be null");
        this.movieDao = Objects.requireNonNull(movieDao, "movieDao must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Showtime getShowtime(long showtimeId) {
        requirePositiveId(showtimeId, "showtimeId");
        try (Connection connection = databaseManager.getConnection()) {
            return requireShowtime(connection, showtimeId);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load showtime", exception);
        }
    }

    public List<Showtime> getUpcomingShowtimes(long movieId) {
        requirePositiveId(movieId, "movieId");
        try (Connection connection = databaseManager.getConnection()) {
            if (movieDao.findById(connection, movieId).isEmpty()) {
                throw new ResourceNotFoundException("Movie", movieId);
            }
            return showtimeDao.findByMovieId(
                    connection, movieId, LocalDateTime.now(clock));
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load showtimes", exception);
        }
    }

    private Showtime requireShowtime(Connection connection, long showtimeId) {
        return showtimeDao.findById(connection, showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", showtimeId));
    }

    private static void requirePositiveId(long value, String name) {
        if (value <= 0) {
            throw new ValidationException(name + " must be greater than zero");
        }
    }
}
