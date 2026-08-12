package edu.nyu.cs6103.movietickets.integration;

import edu.nyu.cs6103.movietickets.client.*;
import edu.nyu.cs6103.movietickets.server.*;
import edu.nyu.cs6103.movietickets.server.config.ServerConfig;
import edu.nyu.cs6103.movietickets.server.dao.*;
import edu.nyu.cs6103.movietickets.server.db.*;
import edu.nyu.cs6103.movietickets.server.security.*;
import edu.nyu.cs6103.movietickets.server.service.*;
import edu.nyu.cs6103.movietickets.shared.*;
import edu.nyu.cs6103.movietickets.shared.dto.*;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.nio.file.*;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class ClientServerIntegrationTest {
    @Test
    void clientCompletesAuthenticatedFlowAgainstRealServer() throws Exception {
        Path db = Files.createTempFile("movie-integration-", ".db");
        Files.deleteIfExists(db);
        int port;
        try (ServerSocket probe = new ServerSocket(0)) { port = probe.getLocalPort(); }
        ServerConfig config = new ServerConfig("127.0.0.1", port, 8,
                "jdbc:sqlite:" + db, 3000, 10000);
        DatabaseManager manager = new DatabaseManager(config);
        new DatabaseInitializer(manager).initialize(
                Path.of("src/test/resources/test-schema.sql"),
                Path.of("src/test/resources/test-seed.sql"));
        TransactionManager transactions = new TransactionManager(manager);
        UserDao users = new UserDao(); MovieDao movies = new MovieDao();
        TheaterDao theaters = new TheaterDao(); SeatDao seats = new SeatDao();
        ShowtimeDao showtimes = new ShowtimeDao(); BookingDao bookings = new BookingDao();
        BookingSeatDao bookingSeats = new BookingSeatDao();
        Clock clock = Clock.fixed(Instant.parse("2029-01-01T00:00:00Z"), ZoneOffset.UTC);
        AuthenticationService auth = new AuthenticationService(manager, transactions, users,
                new PasswordHasher(4), new SessionService(new TokenGenerator()));
        RequestRouter router = new RequestRouter(new JsonCodec(), auth,
                new MovieService(manager, movies), new TheaterService(manager, theaters, seats),
                new ShowtimeService(manager, showtimes, movies, clock),
                new SeatService(manager, showtimes, seats, bookingSeats),
                new BookingService(manager, transactions, users, showtimes, seats,
                        bookings, bookingSeats, clock),
                new AdminService(transactions, movies, theaters, seats, showtimes));
        MovieTicketServer server = new MovieTicketServer(config, router, new JsonCodec());
        Thread thread = new Thread(() -> {
            try { server.start(); }
            catch (Exception exception) { if (server.isRunning()) throw new RuntimeException(exception); }
        });
        thread.start();
        try {
            for (int i = 0; i < 100 && !server.isRunning(); i++) Thread.sleep(20);
            assertTrue(server.isRunning());
            Session session = new Session();
            try (SocketClient client = new SocketClient(config, session)) {
                client.connect();
                assertTrue(client.send(RequestType.REGISTER,
                        new RegisterRequest("integration-user", "password-123")).successful());
                assertTrue(client.login("integration-user", "password-123").successful());
                NetworkResponse response = client.send(RequestType.GET_MOVIES, null);
                assertEquals(1, client.responseData(response, MovieListResponse.class).movies().size());
                assertTrue(client.logout().successful());
                assertFalse(session.isAuthenticated());
            }
        } finally {
            server.stop(); thread.join(10000); Files.deleteIfExists(db);
        }
        assertFalse(thread.isAlive());
    }
}
