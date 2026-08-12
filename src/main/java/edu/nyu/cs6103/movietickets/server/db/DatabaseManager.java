package edu.nyu.cs6103.movietickets.server.db;

import edu.nyu.cs6103.movietickets.server.config.ServerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Creates independently configured SQLite connections.
 *
 * <p>SQLite settings such as foreign key enforcement are connection-scoped, so
 * every connection returned by this class is configured before it is exposed.</p>
 */
public final class DatabaseManager {

    public static final int DEFAULT_BUSY_TIMEOUT_MILLIS = 5_000;
    private static final String SQLITE_URL_PREFIX = "jdbc:sqlite:";

    private final String databaseUrl;
    private final int busyTimeoutMillis;

    public DatabaseManager(ServerConfig config) {
        this(Objects.requireNonNull(config, "config must not be null").databaseUrl());
    }

    public DatabaseManager(String databaseUrl) {
        this(databaseUrl, DEFAULT_BUSY_TIMEOUT_MILLIS);
    }

    public DatabaseManager(String databaseUrl, int busyTimeoutMillis) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("databaseUrl must not be blank");
        }
        if (!databaseUrl.startsWith(SQLITE_URL_PREFIX)) {
            throw new IllegalArgumentException("Only SQLite JDBC URLs are supported");
        }
        if (busyTimeoutMillis <= 0) {
            throw new IllegalArgumentException("busyTimeoutMillis must be greater than zero");
        }

        this.databaseUrl = databaseUrl.trim();
        this.busyTimeoutMillis = busyTimeoutMillis;
        createDatabaseDirectoryIfNeeded();
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);
        try {
            configureConnection(connection);
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public String databaseUrl() {
        return databaseUrl;
    }

    public int busyTimeoutMillis() {
        return busyTimeoutMillis;
    }

    private void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = " + busyTimeoutMillis);

            if (isFileDatabase()) {
                try (ResultSet ignored = statement.executeQuery("PRAGMA journal_mode = WAL")) {
                    // Executing the pragma is sufficient; the result reports the active mode.
                }
            }
        }
    }

    private boolean isFileDatabase() {
        String target = databaseUrl.substring(SQLITE_URL_PREFIX.length());
        return !target.equals(":memory:") && !target.contains("mode=memory");
    }

    private void createDatabaseDirectoryIfNeeded() {
        if (!isFileDatabase()) {
            return;
        }

        String target = databaseUrl.substring(SQLITE_URL_PREFIX.length());
        int queryStart = target.indexOf('?');
        if (queryStart >= 0) {
            target = target.substring(0, queryStart);
        }
        if (target.startsWith("file:")) {
            return;
        }

        Path parent = Path.of(target).toAbsolutePath().normalize().getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create database directory: " + parent, exception);
        }
    }
}
