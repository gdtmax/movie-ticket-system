package edu.nyu.cs6103.movietickets.server.db;

import edu.nyu.cs6103.movietickets.server.config.ServerConfig;
import edu.nyu.cs6103.movietickets.server.dao.UserDao;
import edu.nyu.cs6103.movietickets.server.security.PasswordHasher;
import edu.nyu.cs6103.movietickets.server.security.TokenGenerator;
import edu.nyu.cs6103.movietickets.server.service.AuthenticationService;
import edu.nyu.cs6103.movietickets.server.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class ProductionSeedIntegrationTest {
    @TempDir Path temporaryDirectory;

    @Test void productionSeedCreatesWorkingAccountsAndRollingShowtimes() throws Exception {
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:"
                + temporaryDirectory.resolve("production-seed.db"));
        DatabaseInitializer initializer = new DatabaseInitializer(database);
        Path root = Path.of(System.getProperty("user.dir"));
        Path schema = root.resolve("database/schema.sql");
        Path seed = root.resolve("database/seed.sql");

        initializer.initialize(schema, seed);
        initializer.initialize(schema, seed);

        AuthenticationService authentication = new AuthenticationService(
                database, new TransactionManager(database), new UserDao(),
                new PasswordHasher(4), new SessionService(new TokenGenerator()));
        assertEquals("USER", authentication.login("demo", "password")
                .user().role().toDatabaseValue());
        assertEquals("ADMIN", authentication.login("admin", "password")
                .user().role().toDatabaseValue());

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet users = statement.executeQuery(
                    "SELECT COUNT(*) FROM users WHERE username IN ('demo','admin')")) {
                assertTrue(users.next());
                assertEquals(2, users.getInt(1));
            }
            try (ResultSet showtimes = statement.executeQuery(
                    "SELECT COUNT(*) FROM showtimes WHERE status='SCHEDULED' "
                            + "AND datetime(start_time) > datetime('now')")) {
                assertTrue(showtimes.next());
                assertEquals(4, showtimes.getInt(1));
            }
        }
    }
}
