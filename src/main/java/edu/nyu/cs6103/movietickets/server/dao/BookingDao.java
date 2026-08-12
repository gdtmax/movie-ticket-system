package edu.nyu.cs6103.movietickets.server.dao;

import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.model.Booking;
import edu.nyu.cs6103.movietickets.server.model.BookingStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BookingDao {

    public Booking insert(Connection connection, long userId, long showtimeId,
                          BigDecimal totalPrice) {
        String sql = """
                INSERT INTO bookings (user_id, showtime_id, status, total_price)
                VALUES (?, ?, 'CONFIRMED', ?)
                """;
        try (PreparedStatement statement = DaoSupport.insertStatement(connection, sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, showtimeId);
            statement.setBigDecimal(3, totalPrice);
            statement.executeUpdate();
            return findById(connection, DaoSupport.generatedId(statement)).orElseThrow();
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to create booking", exception);
        }
    }

    public Optional<Booking> findById(Connection connection, long id) {
        String sql = "SELECT * FROM bookings WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to query booking", exception);
        }
    }

    public List<Booking> findByUserId(Connection connection, long userId) {
        String sql = "SELECT * FROM bookings WHERE user_id=? ORDER BY created_at DESC, id DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<Booking> bookings = new ArrayList<>();
                while (result.next()) {
                    bookings.add(map(result));
                }
                return List.copyOf(bookings);
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to list user bookings", exception);
        }
    }

    public boolean cancel(Connection connection, long bookingId, LocalDateTime cancelledAt) {
        String sql = """
                UPDATE bookings
                SET status='CANCELLED', cancelled_at=?
                WHERE id=? AND status='CONFIRMED'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DaoSupport.formatTime(cancelledAt));
            statement.setLong(2, bookingId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to cancel booking", exception);
        }
    }

    private static Booking map(ResultSet result) throws SQLException {
        return new Booking(
                result.getLong("id"), result.getLong("user_id"),
                result.getLong("showtime_id"),
                BookingStatus.fromDatabaseValue(result.getString("status")),
                result.getBigDecimal("total_price"),
                DaoSupport.readTime(result, "created_at"),
                DaoSupport.readTime(result, "cancelled_at"));
    }
}
