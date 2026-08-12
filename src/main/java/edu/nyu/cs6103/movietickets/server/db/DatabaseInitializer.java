package edu.nyu.cs6103.movietickets.server.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Executes the database schema and optional development seed as one transaction. */
public final class DatabaseInitializer {

    private final DatabaseManager databaseManager;

    public DatabaseInitializer(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public void initialize(Path schemaPath) throws SQLException, IOException {
        initialize(schemaPath, null);
    }

    public void initialize(Path schemaPath, Path seedPath) throws SQLException, IOException {
        Objects.requireNonNull(schemaPath, "schemaPath must not be null");

        List<String> schemaStatements = readStatements(schemaPath);
        List<String> seedStatements = seedPath == null ? List.of() : readStatements(seedPath);

        try (Connection connection = databaseManager.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                executeStatements(connection, schemaStatements);
                executeStatements(connection, seedStatements);
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public boolean isInitialized() throws SQLException {
        String sql = "SELECT 1 FROM sqlite_master WHERE type='table' AND name='users'";
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next();
        }
    }

    static List<String> readStatements(Path scriptPath) throws IOException {
        if (!Files.isRegularFile(scriptPath)) {
            throw new IOException("SQL script not found: " + scriptPath);
        }

        String script = Files.readString(scriptPath);
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideTrigger = false;

        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            if (trimmed.toUpperCase().startsWith("CREATE TRIGGER")) {
                insideTrigger = true;
            }

            current.append(line).append('\n');
            boolean statementComplete = !insideTrigger && trimmed.endsWith(";");
            boolean triggerComplete = insideTrigger && trimmed.equalsIgnoreCase("END;");
            if (statementComplete || triggerComplete) {
                statements.add(current.toString());
                current.setLength(0);
                insideTrigger = false;
            }
        }

        if (!current.toString().isBlank()) {
            throw new IOException("Unterminated SQL statement in: " + scriptPath);
        }
        return List.copyOf(statements);
    }

    private static void executeStatements(Connection connection, List<String> statements)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static void rollback(Connection connection, SQLException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }
}
