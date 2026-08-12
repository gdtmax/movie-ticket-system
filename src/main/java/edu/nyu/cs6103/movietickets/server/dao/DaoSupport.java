package edu.nyu.cs6103.movietickets.server.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class DaoSupport {

    private static final DateTimeFormatter DATABASE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DaoSupport() {
    }

    static String formatTime(LocalDateTime value) {
        return value.format(DATABASE_TIME);
    }

    static LocalDateTime readTime(ResultSet result, String column) throws SQLException {
        String value = result.getString(column);
        return value == null ? null : LocalDateTime.parse(value, DATABASE_TIME);
    }

    static long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("Database did not return a generated identifier");
            }
            return keys.getLong(1);
        }
    }

    static PreparedStatement insertStatement(java.sql.Connection connection, String sql)
            throws SQLException {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }
}
