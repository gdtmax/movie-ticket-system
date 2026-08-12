package edu.nyu.cs6103.movietickets.server.dao;

import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.model.Theater;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TheaterDao {

    public Theater insert(Connection connection, String name, String location) {
        String sql = "INSERT INTO theaters (name, location) VALUES (?, ?)";
        try (PreparedStatement statement = DaoSupport.insertStatement(connection, sql)) {
            statement.setString(1, name);
            statement.setString(2, location);
            statement.executeUpdate();
            return findById(connection, DaoSupport.generatedId(statement)).orElseThrow();
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to create theater", exception);
        }
    }

    public Optional<Theater> findById(Connection connection, long id) {
        String sql = "SELECT * FROM theaters WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to query theater", exception);
        }
    }

    public List<Theater> findAll(Connection connection) {
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT * FROM theaters ORDER BY name, id");
             ResultSet result = statement.executeQuery()) {
            List<Theater> theaters = new ArrayList<>();
            while (result.next()) {
                theaters.add(map(result));
            }
            return List.copyOf(theaters);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to list theaters", exception);
        }
    }

    public boolean update(Connection connection, Theater theater) {
        String sql = "UPDATE theaters SET name=?, location=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, theater.name());
            statement.setString(2, theater.location());
            statement.setLong(3, theater.id());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to update theater", exception);
        }
    }

    public boolean delete(Connection connection, long id) {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM theaters WHERE id=?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to delete theater", exception);
        }
    }

    private static Theater map(ResultSet result) throws SQLException {
        return new Theater(
                result.getLong("id"), result.getString("name"),
                result.getString("location"), DaoSupport.readTime(result, "created_at"));
    }
}
