package edu.nyu.cs6103.movietickets.server;

import edu.nyu.cs6103.movietickets.server.config.ServerConfig;
import edu.nyu.cs6103.movietickets.server.config.ApplicationPaths;
import edu.nyu.cs6103.movietickets.server.dao.BookingDao;
import edu.nyu.cs6103.movietickets.server.dao.BookingSeatDao;
import edu.nyu.cs6103.movietickets.server.dao.MovieDao;
import edu.nyu.cs6103.movietickets.server.dao.SeatDao;
import edu.nyu.cs6103.movietickets.server.dao.ShowtimeDao;
import edu.nyu.cs6103.movietickets.server.dao.TheaterDao;
import edu.nyu.cs6103.movietickets.server.dao.UserDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseInitializer;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.db.TransactionManager;
import edu.nyu.cs6103.movietickets.server.security.PasswordHasher;
import edu.nyu.cs6103.movietickets.server.security.TokenGenerator;
import edu.nyu.cs6103.movietickets.server.service.AuthenticationService;
import edu.nyu.cs6103.movietickets.server.service.AdminService;
import edu.nyu.cs6103.movietickets.server.service.BookingService;
import edu.nyu.cs6103.movietickets.server.service.MovieService;
import edu.nyu.cs6103.movietickets.server.service.SeatService;
import edu.nyu.cs6103.movietickets.server.service.SessionService;
import edu.nyu.cs6103.movietickets.server.service.ShowtimeService;
import edu.nyu.cs6103.movietickets.server.service.TheaterService;
import edu.nyu.cs6103.movietickets.shared.JsonCodec;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Multithreaded TCP server entry point. */
public final class MovieTicketServer implements AutoCloseable {

    private static final System.Logger LOGGER =
            System.getLogger(MovieTicketServer.class.getName());

    private final ServerConfig config;
    private final RequestRouter requestRouter;
    private final JsonCodec codec;
    private final ExecutorService clientExecutor;

    private volatile boolean running;
    private volatile ServerSocket serverSocket;

    public MovieTicketServer(ServerConfig config, RequestRouter requestRouter, JsonCodec codec) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.requestRouter = Objects.requireNonNull(requestRouter, "requestRouter must not be null");
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
        clientExecutor = Executors.newFixedThreadPool(config.threadPoolSize(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("movie-ticket-client-" + thread.threadId());
            thread.setDaemon(false);
            return thread;
        });
    }

    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        ServerSocket listeningSocket = new ServerSocket();
        listeningSocket.setReuseAddress(true);
        listeningSocket.bind(new InetSocketAddress(config.host(), config.port()));
        serverSocket = listeningSocket;
        running = true;
        LOGGER.log(System.Logger.Level.INFO,
                "Movie ticket server listening on " + config.host() + ":" + boundPort());

        try {
            while (running) {
                Socket client = listeningSocket.accept();
                client.setSoTimeout(config.socketReadTimeoutMillis());
                client.setTcpNoDelay(true);
                clientExecutor.execute(new ClientHandler(client, requestRouter, codec));
            }
        } catch (SocketException exception) {
            if (running) {
                throw exception;
            }
        } finally {
            running = false;
            closeListeningSocket();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int boundPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? -1 : socket.getLocalPort();
    }

    public void stop() {
        running = false;
        closeListeningSocket();
        clientExecutor.shutdown();
        try {
            if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                clientExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            clientExecutor.shutdownNow();
        }
    }

    @Override
    public void close() {
        stop();
    }

    private void closeListeningSocket() {
        ServerSocket socket = serverSocket;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Unable to close server socket", exception);
            }
        }
    }

    public static void main(String[] args) {
        Path projectRoot = ApplicationPaths.projectRoot();
        ServerConfig loadedConfig = ServerConfig.load();
        ServerConfig config = new ServerConfig(
                loadedConfig.host(), loadedConfig.port(), loadedConfig.threadPoolSize(),
                ApplicationPaths.resolveDatabaseUrl(loadedConfig.databaseUrl(), projectRoot),
                loadedConfig.socketConnectTimeoutMillis(), loadedConfig.socketReadTimeoutMillis());
        try {
            DatabaseManager databaseManager = new DatabaseManager(config);
            DatabaseInitializer initializer = new DatabaseInitializer(databaseManager);
            initializer.initialize(
                    projectRoot.resolve("database/schema.sql"),
                    projectRoot.resolve("database/seed.sql"));

            JsonCodec codec = new JsonCodec();
            UserDao userDao = new UserDao();
            MovieDao movieDao = new MovieDao();
            TheaterDao theaterDao = new TheaterDao();
            SeatDao seatDao = new SeatDao();
            ShowtimeDao showtimeDao = new ShowtimeDao();
            BookingDao bookingDao = new BookingDao();
            BookingSeatDao bookingSeatDao = new BookingSeatDao();
            TransactionManager transactionManager = new TransactionManager(databaseManager);

            SessionService sessionService = new SessionService(new TokenGenerator());
            AuthenticationService authenticationService = new AuthenticationService(
                    databaseManager, transactionManager, userDao,
                    new PasswordHasher(), sessionService);
            MovieService movieService = new MovieService(databaseManager, movieDao);
            TheaterService theaterService = new TheaterService(
                    databaseManager, theaterDao, seatDao);
            ShowtimeService showtimeService = new ShowtimeService(
                    databaseManager, showtimeDao, movieDao);
            SeatService seatService = new SeatService(
                    databaseManager, showtimeDao, seatDao, bookingSeatDao);
            BookingService bookingService = new BookingService(
                    databaseManager, transactionManager, userDao, showtimeDao,
                    seatDao, bookingDao, bookingSeatDao);
            AdminService adminService = new AdminService(
                    transactionManager, movieDao, theaterDao, seatDao, showtimeDao);

            RequestRouter router = new RequestRouter(
                    codec, authenticationService, movieService, theaterService,
                    showtimeService, seatService, bookingService, adminService);
            MovieTicketServer server = new MovieTicketServer(config, router, codec);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "server-shutdown"));
            server.start();
        } catch (IOException | SQLException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Unable to start server", exception);
            System.exit(1);
        }
    }
}
