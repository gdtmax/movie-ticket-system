package edu.nyu.cs6103.movietickets.concurrency;

import edu.nyu.cs6103.movietickets.server.dao.BookingDao;
import edu.nyu.cs6103.movietickets.server.dao.BookingSeatDao;
import edu.nyu.cs6103.movietickets.server.dao.SeatDao;
import edu.nyu.cs6103.movietickets.server.dao.ShowtimeDao;
import edu.nyu.cs6103.movietickets.server.dao.UserDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseInitializer;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.db.TransactionManager;
import edu.nyu.cs6103.movietickets.server.exception.SeatAlreadyBookedException;
import edu.nyu.cs6103.movietickets.server.model.Seat;
import edu.nyu.cs6103.movietickets.server.model.UserRole;
import edu.nyu.cs6103.movietickets.server.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentBookingTest {

    private static final int CONCURRENT_USERS = 50;

    @TempDir
    Path tempDirectory;

    private DatabaseManager databaseManager;
    private TransactionManager transactionManager;
    private BookingService bookingService;
    private BookingSeatDao bookingSeatDao;
    private List<Long> userIds;
    private List<Long> seatIds;

    @BeforeEach
    void setUp() throws Exception {
        databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + tempDirectory.resolve("concurrency.db"));
        new DatabaseInitializer(databaseManager).initialize(
                testResource("test-schema.sql"), testResource("test-seed.sql"));
        transactionManager = new TransactionManager(databaseManager);

        UserDao userDao = new UserDao();
        SeatDao seatDao = new SeatDao();
        ShowtimeDao showtimeDao = new ShowtimeDao();
        BookingDao bookingDao = new BookingDao();
        bookingSeatDao = new BookingSeatDao();
        bookingService = new BookingService(
                databaseManager, transactionManager, userDao, showtimeDao, seatDao,
                bookingDao, bookingSeatDao,
                Clock.fixed(Instant.parse("2029-01-01T00:00:00Z"), ZoneOffset.UTC));

        userIds = createUsers(userDao);
        seatIds = createFiftySeats(seatDao);
    }

    @Test
    void exactlyOneOfFiftyUsersCanBookTheSameSeat() throws Exception {
        long contestedSeatId = seatIds.getFirst();
        List<AttemptResult> results = runSimultaneously(index -> {
            try {
                bookingService.createBooking(userIds.get(index), 1, List.of(contestedSeatId));
                return AttemptResult.SUCCESS;
            } catch (SeatAlreadyBookedException expected) {
                return AttemptResult.CONFLICT;
            }
        });

        assertEquals(1, count(results, AttemptResult.SUCCESS));
        assertEquals(CONCURRENT_USERS - 1, count(results, AttemptResult.CONFLICT));
        assertEquals(1, confirmedSeatCount());
    }

    @Test
    void fiftyUsersCanBookFiftyDifferentSeatsConcurrently() throws Exception {
        List<AttemptResult> results = runSimultaneously(index -> {
            bookingService.createBooking(
                    userIds.get(index), 1, List.of(seatIds.get(index)));
            return AttemptResult.SUCCESS;
        });

        assertEquals(CONCURRENT_USERS, count(results, AttemptResult.SUCCESS));
        assertEquals(CONCURRENT_USERS, confirmedSeatCount());
    }

    private List<Long> createUsers(UserDao userDao) throws Exception {
        return transactionManager.execute(connection -> {
            List<Long> ids = new ArrayList<>();
            ids.add(1L);
            ids.add(2L);
            for (int index = 3; index <= CONCURRENT_USERS; index++) {
                ids.add(userDao.insert(
                        connection, "concurrent-user-" + index, "test-hash", UserRole.USER).id());
            }
            return List.copyOf(ids);
        });
    }

    private List<Long> createFiftySeats(SeatDao seatDao) throws Exception {
        return transactionManager.execute(connection -> {
            List<Seat> seats = new ArrayList<>(seatDao.findByTheaterId(connection, 1));
            for (int number = seats.size() + 1; number <= CONCURRENT_USERS; number++) {
                seats.add(seatDao.insert(connection, 1, "A", number, false));
            }
            return seats.stream().map(Seat::id).toList();
        });
    }

    private List<AttemptResult> runSimultaneously(Attempt attempt) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<AttemptResult>> futures = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_USERS; index++) {
                int taskIndex = index;
                Callable<AttemptResult> task = () -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return attempt.run(taskIndex);
                };
                futures.add(executor.submit(task));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            List<AttemptResult> results = new ArrayList<>();
            for (Future<AttemptResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return List.copyOf(results);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private long confirmedSeatCount() throws Exception {
        try (var connection = databaseManager.getConnection()) {
            return bookingSeatDao.findConfirmedByShowtimeId(connection, 1).size();
        }
    }

    private static long count(List<AttemptResult> results, AttemptResult expected) {
        return results.stream().filter(expected::equals).count();
    }

    private static Path testResource(String name) throws Exception {
        return Path.of(ConcurrentBookingTest.class.getClassLoader().getResource(name).toURI());
    }

    private enum AttemptResult {
        SUCCESS,
        CONFLICT
    }

    @FunctionalInterface
    private interface Attempt {
        AttemptResult run(int index) throws Exception;
    }
}
