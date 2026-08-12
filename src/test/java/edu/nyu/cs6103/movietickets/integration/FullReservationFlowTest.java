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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FullReservationFlowTest {
    @Test void locksBooksListsCancelsAndReleasesSeatOverRealSockets() throws Exception {
        Path db = Files.createTempFile("movie-full-flow-", ".db");
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
        Thread serverThread = new Thread(() -> {
            try { server.start(); }
            catch (Exception exception) { if (server.isRunning()) throw new RuntimeException(exception); }
        });
        serverThread.start();
        try {
            for (int i = 0; i < 100 && !server.isRunning(); i++) Thread.sleep(20);
            assertTrue(server.isRunning());
            try (SocketClient first = new SocketClient(config, new Session());
                 SocketClient second = new SocketClient(config, new Session())) {
                first.connect(); second.connect();
                registerAndLogin(first, "flow-first");
                registerAndLogin(second, "flow-second");

                NetworkResponse showtimeResponse = first.send(
                        RequestType.GET_SHOWTIMES, new ShowtimeRequest(1));
                assertEquals(2, first.responseData(showtimeResponse,
                        ShowtimeListResponse.class).showtimes().size());

                SeatLockRequest hold = new SeatLockRequest(1, List.of(1L));
                assertTrue(first.send(RequestType.LOCK_SEATS, hold).successful());
                NetworkResponse competingHold = second.send(RequestType.LOCK_SEATS, hold);
                assertFalse(competingHold.successful());
                assertEquals("SEAT_ALREADY_BOOKED", competingHold.error().code());

                NetworkResponse created = first.send(RequestType.CREATE_BOOKING,
                        new CreateBookingRequest(1, List.of(1L)));
                assertTrue(created.successful());
                long bookingId = first.responseData(created, BookingResponse.class).booking().id();

                NetworkResponse history = first.send(RequestType.GET_BOOKING_HISTORY,
                        new BookingHistoryRequest());
                assertEquals(1, first.responseData(history, BookingResponse.class).bookings().size());
                SeatMapResponse bookedMap = first.responseData(first.send(RequestType.GET_SEAT_MAP,
                        new SeatMapRequest(1)), SeatMapResponse.class);
                assertEquals("BOOKED", bookedMap.seats().stream()
                        .filter(seat -> seat.id() == 1).findFirst().orElseThrow().status());

                assertTrue(first.send(RequestType.CANCEL_BOOKING,
                        new CancelBookingRequest(bookingId)).successful());
                SeatMapResponse releasedMap = second.responseData(second.send(RequestType.GET_SEAT_MAP,
                        new SeatMapRequest(1)), SeatMapResponse.class);
                assertEquals("AVAILABLE", releasedMap.seats().stream()
                        .filter(seat -> seat.id() == 1).findFirst().orElseThrow().status());
            }
        } finally {
            server.stop(); serverThread.join(10000); Files.deleteIfExists(db);
        }
        assertFalse(serverThread.isAlive());
    }

    private static void registerAndLogin(SocketClient client, String username) throws Exception {
        assertTrue(client.send(RequestType.REGISTER,
                new RegisterRequest(username, "password-123")).successful());
        assertTrue(client.login(username, "password-123").successful());
    }
}
