package edu.nyu.cs6103.movietickets.server.dao;

import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.model.User;
import edu.nyu.cs6103.movietickets.server.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class UserDao {

    public User insert(Connection connection, String username, String passwordHash, UserRole role) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (PreparedStatement statement = DaoSupport.insertStatement(connection, sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.toDatabaseValue());
            statement.executeUpdate();
            return findById(connection, DaoSupport.generatedId(statement)).orElseThrow();
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to create user", exception);
        }
    }

    public Optional<User> findById(Connection connection, long id) {
        return findOne(connection, "SELECT * FROM users WHERE id = ?", id);
    }

    public Optional<User> findByUsername(Connection connection, String username) {
        return findOne(connection, "SELECT * FROM users WHERE username = ? COLLATE NOCASE", username);
    }

    public boolean updateRole(Connection connection, long id, UserRole role) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.toDatabaseValue());
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to update user role", exception);
        }
    }

    private Optional<User> findOne(Connection connection, String sql, Object value) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to query user", exception);
        }
    }

    private static User map(ResultSet result) throws SQLException {
        return new User(
                result.getLong("id"),
                result.getString("username"),
                result.getString("password_hash"),
                UserRole.fromDatabaseValue(result.getString("role")),
                DaoSupport.readTime(result, "created_at"));
    }
}
