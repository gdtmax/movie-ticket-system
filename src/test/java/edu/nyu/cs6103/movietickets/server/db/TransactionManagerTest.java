package edu.nyu.cs6103.movietickets.server.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionManagerTest {

    @TempDir
    Path tempDirectory;

    private DatabaseManager databaseManager;
    private TransactionManager transactionManager;

    @BeforeEach
    void setUp() throws Exception {
        databaseManager = new DatabaseManager("jdbc:sqlite:" + tempDirectory.resolve("transactions.db"));
        transactionManager = new TransactionManager(databaseManager);
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE values_under_test (value INTEGER NOT NULL)");
        }
    }

    @Test
    void commitsSuccessfulWorkAndReturnsItsResult() throws Exception {
        String result = transactionManager.execute(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO values_under_test VALUES (1)");
            }
            return "committed";
        });

        assertEquals("committed", result);
        assertEquals(1, rowCount());
    }

    @Test
    void rollsBackAllWorkWhenAnOperationFails() throws Exception {
        assertThrows(SQLException.class, () ->
                transactionManager.executeWithoutResult(connection -> {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("INSERT INTO values_under_test VALUES (1)");
                    }
                    throw new SQLException("expected failure");
                }));

        assertEquals(0, rowCount());
    }

    private long rowCount() throws Exception {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM values_under_test")) {
            result.next();
            return result.getLong(1);
        }
    }
}
