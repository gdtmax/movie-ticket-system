package edu.nyu.cs6103.movietickets.server.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsParentDirectoryAndConfiguresEveryConnection() throws Exception {
        Path databasePath = tempDirectory.resolve("nested/test.db");
        DatabaseManager manager = new DatabaseManager("jdbc:sqlite:" + databasePath);

        try (Connection connection = manager.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals(1, pragmaInt(statement, "foreign_keys"));
            assertEquals(DatabaseManager.DEFAULT_BUSY_TIMEOUT_MILLIS,
                    pragmaInt(statement, "busy_timeout"));
            assertEquals("wal", pragmaText(statement, "journal_mode"));
        }

        assertTrue(Files.isRegularFile(databasePath));
    }

    private static int pragmaInt(Statement statement, String pragma) throws Exception {
        try (ResultSet result = statement.executeQuery("PRAGMA " + pragma)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static String pragmaText(Statement statement, String pragma) throws Exception {
        try (ResultSet result = statement.executeQuery("PRAGMA " + pragma)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
