package edu.nyu.cs6103.movietickets.server.dao;

import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.model.Movie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MovieDao {

    public Movie insert(Connection connection, Movie movie) {
        String sql = """
                INSERT INTO movies
                    (title, duration_minutes, description, genre, poster_path, active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = DaoSupport.insertStatement(connection, sql)) {
            bindMutableFields(statement, movie);
            statement.executeUpdate();
            return findById(connection, DaoSupport.generatedId(statement)).orElseThrow();
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to create movie", exception);
        }
    }

    public Optional<Movie> findById(Connection connection, long id) {
        String sql = "SELECT * FROM movies WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to query movie", exception);
        }
    }

    public List<Movie> findAll(Connection connection, boolean activeOnly) {
        String sql = activeOnly
                ? "SELECT * FROM movies WHERE active = 1 ORDER BY title, id"
                : "SELECT * FROM movies ORDER BY title, id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Movie> movies = new ArrayList<>();
            while (result.next()) {
                movies.add(map(result));
            }
            return List.copyOf(movies);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to list movies", exception);
        }
    }

    public boolean update(Connection connection, Movie movie) {
        String sql = """
                UPDATE movies
                SET title=?, duration_minutes=?, description=?, genre=?, poster_path=?, active=?
                WHERE id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMutableFields(statement, movie);
            statement.setLong(7, movie.id());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to update movie", exception);
        }
    }

    public boolean setActive(Connection connection, long id, boolean active) {
        try (PreparedStatement statement =
                     connection.prepareStatement("UPDATE movies SET active=? WHERE id=?")) {
            statement.setInt(1, active ? 1 : 0);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to change movie availability", exception);
        }
    }

    private static void bindMutableFields(PreparedStatement statement, Movie movie)
            throws SQLException {
        statement.setString(1, movie.title());
        statement.setInt(2, movie.durationMinutes());
        statement.setString(3, movie.description());
        statement.setString(4, movie.genre());
        statement.setString(5, movie.posterPath());
        statement.setInt(6, movie.active() ? 1 : 0);
    }

    private static Movie map(ResultSet result) throws SQLException {
        return new Movie(
                result.getLong("id"), result.getString("title"),
                result.getInt("duration_minutes"), result.getString("description"),
                result.getString("genre"), result.getString("poster_path"),
                result.getInt("active") == 1, DaoSupport.readTime(result, "created_at"));
    }
}
