package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.MovieDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.Movie;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class MovieService {

    private final DatabaseManager databaseManager;
    private final MovieDao movieDao;

    public MovieService(DatabaseManager databaseManager, MovieDao movieDao) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.movieDao = Objects.requireNonNull(movieDao, "movieDao must not be null");
    }

    public Movie getMovie(long movieId) {
        requirePositiveId(movieId, "movieId");
        try (Connection connection = databaseManager.getConnection()) {
            return movieDao.findById(connection, movieId)
                    .orElseThrow(() -> new ResourceNotFoundException("Movie", movieId));
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load movie", exception);
        }
    }

    public List<Movie> getAvailableMovies() {
        return findAll(true);
    }

    public List<Movie> getAllMovies() {
        return findAll(false);
    }

    private List<Movie> findAll(boolean activeOnly) {
        try (Connection connection = databaseManager.getConnection()) {
            return movieDao.findAll(connection, activeOnly);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to load movies", exception);
        }
    }

    private static void requirePositiveId(long value, String name) {
        if (value <= 0) {
            throw new ValidationException(name + " must be greater than zero");
        }
    }
}
