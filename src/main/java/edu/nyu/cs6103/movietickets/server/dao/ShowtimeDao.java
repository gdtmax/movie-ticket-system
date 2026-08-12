package edu.nyu.cs6103.movietickets.server.dao;

import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.model.Showtime;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShowtimeDao {

    public Showtime insert(Connection connection, long movieId, long theaterId,
                           LocalDateTime startTime, BigDecimal price) {
        String sql = """
                INSERT INTO showtimes (movie_id, theater_id, start_time, price)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = DaoSupport.insertStatement(connection, sql)) {
            statement.setLong(1, movieId);
            statement.setLong(2, theaterId);
            statement.setString(3, DaoSupport.formatTime(startTime));
            statement.setBigDecimal(4, price);
            statement.executeUpdate();
            return findById(connection, DaoSupport.generatedId(statement)).orElseThrow();
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to create showtime", exception);
        }
    }

    public Optional<Showtime> findById(Connection connection, long id) {
        String sql = "SELECT * FROM showtimes WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to query showtime", exception);
        }
    }

    public List<Showtime> findByMovieId(Connection connection, long movieId,
                                         LocalDateTime startingAt) {
        String sql = """
                SELECT * FROM showtimes
                WHERE movie_id=? AND status='SCHEDULED' AND start_time>=?
                ORDER BY start_time, id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            statement.setString(2, DaoSupport.formatTime(startingAt));
            try (ResultSet result = statement.executeQuery()) {
                List<Showtime> showtimes = new ArrayList<>();
                while (result.next()) {
                    showtimes.add(map(result));
                }
                return List.copyOf(showtimes);
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to list movie showtimes", exception);
        }
    }

    public boolean update(Connection connection, Showtime showtime) {
        String sql = """
                UPDATE showtimes
                SET movie_id=?, theater_id=?, start_time=?, price=?, status=?
                WHERE id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, showtime.movieId());
            statement.setLong(2, showtime.theaterId());
            statement.setString(3, DaoSupport.formatTime(showtime.startTime()));
            statement.setBigDecimal(4, showtime.price());
            statement.setString(5, showtime.status().toDatabaseValue());
            statement.setLong(6, showtime.id());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to update showtime", exception);
        }
    }

    public boolean cancel(Connection connection, long id) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE showtimes SET status='CANCELLED' WHERE id=? AND status='SCHEDULED'")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to cancel showtime", exception);
        }
    }

    private static Showtime map(ResultSet result) throws SQLException {
        return new Showtime(
                result.getLong("id"), result.getLong("movie_id"),
                result.getLong("theater_id"), DaoSupport.readTime(result, "start_time"),
                result.getBigDecimal("price"),
                Showtime.Status.fromDatabaseValue(result.getString("status")),
                DaoSupport.readTime(result, "created_at"));
    }
}
