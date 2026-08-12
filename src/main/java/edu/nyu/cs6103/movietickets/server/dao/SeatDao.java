package edu.nyu.cs6103.movietickets.server.dao;

import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.model.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SeatDao {

    public Seat insert(Connection connection, long theaterId, String rowLabel,
                       int seatNumber, boolean accessible) {
        String sql = """
                INSERT INTO seats (theater_id, row_label, seat_number, is_accessible)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = DaoSupport.insertStatement(connection, sql)) {
            statement.setLong(1, theaterId);
            statement.setString(2, rowLabel);
            statement.setInt(3, seatNumber);
            statement.setInt(4, accessible ? 1 : 0);
            statement.executeUpdate();
            return findById(connection, DaoSupport.generatedId(statement)).orElseThrow();
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to create seat", exception);
        }
    }

    public Optional<Seat> findById(Connection connection, long id) {
        String sql = "SELECT * FROM seats WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to query seat", exception);
        }
    }

    public List<Seat> findByTheaterId(Connection connection, long theaterId) {
        String sql = """
                SELECT * FROM seats
                WHERE theater_id=?
                ORDER BY row_label, seat_number
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, theaterId);
            try (ResultSet result = statement.executeQuery()) {
                List<Seat> seats = new ArrayList<>();
                while (result.next()) {
                    seats.add(map(result));
                }
                return List.copyOf(seats);
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to list theater seats", exception);
        }
    }

    public boolean update(Connection connection, Seat seat) {
        String sql = """
                UPDATE seats
                SET theater_id=?, row_label=?, seat_number=?, is_accessible=?
                WHERE id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, seat.theaterId());
            statement.setString(2, seat.rowLabel());
            statement.setInt(3, seat.seatNumber());
            statement.setInt(4, seat.accessible() ? 1 : 0);
            statement.setLong(5, seat.id());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to update seat", exception);
        }
    }

    public boolean delete(Connection connection, long id) {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM seats WHERE id=?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to delete seat", exception);
        }
    }

    private static Seat map(ResultSet result) throws SQLException {
        return new Seat(
                result.getLong("id"), result.getLong("theater_id"),
                result.getString("row_label"), result.getInt("seat_number"),
                result.getInt("is_accessible") == 1);
    }
}
