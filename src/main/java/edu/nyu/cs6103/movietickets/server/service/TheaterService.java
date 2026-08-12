package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.SeatDao;
import edu.nyu.cs6103.movietickets.server.dao.TheaterDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.Seat;
import edu.nyu.cs6103.movietickets.server.model.Theater;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class TheaterService {

    private final DatabaseManager databaseManager;
    private final TheaterDao theaterDao;
    private final SeatDao seatDao;

    public TheaterService(
            DatabaseManager databaseManager, TheaterDao theaterDao, SeatDao seatDao) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.theaterDao = Objects.requireNonNull(theaterDao, "theaterDao must not be null");
        this.seatDao = Objects.requireNonNull(seatDao, "seatDao must not be null");
    }

    public Theater getTheater(long theaterId) {
        requirePositiveId(theaterId, "theaterId");
        try (Connection connection = databaseManager.getConnection()) {
            return requireTheater(connection, theaterId);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load theater", exception);
        }
    }

    public List<Theater> getAllTheaters() {
        try (Connection connection = databaseManager.getConnection()) {
            return theaterDao.findAll(connection);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load theaters", exception);
        }
    }

    public List<Seat> getTheaterSeats(long theaterId) {
        requirePositiveId(theaterId, "theaterId");
        try (Connection connection = databaseManager.getConnection()) {
            requireTheater(connection, theaterId);
            return seatDao.findByTheaterId(connection, theaterId);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load theater seats", exception);
        }
    }

    private Theater requireTheater(Connection connection, long theaterId) {
        return theaterDao.findById(connection, theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater", theaterId));
    }

    private static void requirePositiveId(long value, String name) {
        if (value <= 0) {
            throw new ValidationException(name + " must be greater than zero");
        }
    }
}
