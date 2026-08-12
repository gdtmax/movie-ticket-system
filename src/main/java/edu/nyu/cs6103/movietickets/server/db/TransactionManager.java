package edu.nyu.cs6103.movietickets.server.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Runs a unit of database work using one connection and one transaction. */
public final class TransactionManager {

    private final DatabaseManager databaseManager;

    public TransactionManager(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public <T> T execute(TransactionWork<T> work) throws SQLException {
        Objects.requireNonNull(work, "work must not be null");

        try (Connection connection = databaseManager.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public void executeWithoutResult(TransactionAction action) throws SQLException {
        Objects.requireNonNull(action, "action must not be null");
        execute(connection -> {
            action.execute(connection);
            return null;
        });
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionAction {
        void execute(Connection connection) throws SQLException;
    }
}
