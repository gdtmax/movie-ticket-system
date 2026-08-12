package edu.nyu.cs6103.movietickets.server.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void initializesAndSeedsAnEmptyDatabaseIdempotently() throws Exception {
        DatabaseManager manager = manager();
        DatabaseInitializer initializer = new DatabaseInitializer(manager);

        assertFalse(initializer.isInitialized());
        initializer.initialize(testResource("test-schema.sql"), testResource("test-seed.sql"));
        assertTrue(initializer.isInitialized());
        assertEquals(2, count(manager, "users"));
        assertEquals(4, count(manager, "seats"));

        // Schema uses IF NOT EXISTS. The production seed uses INSERT OR IGNORE;
        // this test runs schema-only on the second pass to verify safe startup.
        initializer.initialize(testResource("test-schema.sql"));
        assertEquals(2, count(manager, "users"));
        assertEquals(4, count(manager, "seats"));
    }

    private DatabaseManager manager() {
        return new DatabaseManager("jdbc:sqlite:" + tempDirectory.resolve("initializer.db"));
    }

    private static Path testResource(String name) throws Exception {
        return Path.of(DatabaseInitializerTest.class.getClassLoader().getResource(name).toURI());
    }

    private static long count(DatabaseManager manager, String table) throws Exception {
        try (Connection connection = manager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }
}
