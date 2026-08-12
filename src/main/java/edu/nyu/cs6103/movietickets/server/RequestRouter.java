package edu.nyu.cs6103.movietickets.server;

import edu.nyu.cs6103.movietickets.server.concurrency.SeatLockManager;
import edu.nyu.cs6103.movietickets.server.exception.AuthenticationException;
import edu.nyu.cs6103.movietickets.server.exception.AuthorizationException;
import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.SeatAlreadyBookedException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.BookingSeat;
import edu.nyu.cs6103.movietickets.server.model.Movie;
import edu.nyu.cs6103.movietickets.server.model.Showtime;
import edu.nyu.cs6103.movietickets.server.model.Theater;
import edu.nyu.cs6103.movietickets.server.model.User;
import edu.nyu.cs6103.movietickets.server.service.AuthenticationService;
import edu.nyu.cs6103.movietickets.server.service.AdminService;
import edu.nyu.cs6103.movietickets.server.service.BookingService;
import edu.nyu.cs6103.movietickets.server.service.MovieService;
import edu.nyu.cs6103.movietickets.server.service.SeatService;
import edu.nyu.cs6103.movietickets.server.service.ShowtimeService;
import edu.nyu.cs6103.movietickets.server.service.TheaterService;
import edu.nyu.cs6103.movietickets.shared.JsonCodec;
import edu.nyu.cs6103.movietickets.shared.NetworkRequest;
import edu.nyu.cs6103.movietickets.shared.NetworkResponse;
import edu.nyu.cs6103.movietickets.shared.dto.BookingDto;
import edu.nyu.cs6103.movietickets.shared.dto.BookingHistoryRequest;
import edu.nyu.cs6103.movietickets.shared.dto.BookingResponse;
import edu.nyu.cs6103.movietickets.shared.dto.CancelBookingRequest;
import edu.nyu.cs6103.movietickets.shared.dto.CreateBookingRequest;
import edu.nyu.cs6103.movietickets.shared.dto.ErrorResponse;
import edu.nyu.cs6103.movietickets.shared.dto.LoginRequest;
import edu.nyu.cs6103.movietickets.shared.dto.LoginResponse;
import edu.nyu.cs6103.movietickets.shared.dto.LogoutRequest;
import edu.nyu.cs6103.movietickets.shared.dto.MovieDto;
import edu.nyu.cs6103.movietickets.shared.dto.MovieListResponse;
import edu.nyu.cs6103.movietickets.shared.dto.MovieRequest;
import edu.nyu.cs6103.movietickets.shared.dto.RegisterRequest;
import edu.nyu.cs6103.movietickets.shared.dto.RegisterResponse;
import edu.nyu.cs6103.movietickets.shared.dto.SeatDto;
import edu.nyu.cs6103.movietickets.shared.dto.SeatMapRequest;
import edu.nyu.cs6103.movietickets.shared.dto.SeatMapResponse;
import edu.nyu.cs6103.movietickets.shared.dto.SeatLockRequest;
import edu.nyu.cs6103.movietickets.shared.dto.SeatLockResponse;
import edu.nyu.cs6103.movietickets.shared.dto.ShowtimeDto;
import edu.nyu.cs6103.movietickets.shared.dto.ShowtimeListResponse;
import edu.nyu.cs6103.movietickets.shared.dto.ShowtimeRequest;
import edu.nyu.cs6103.movietickets.shared.dto.TheaterDto;
import edu.nyu.cs6103.movietickets.shared.dto.TheaterRequest;
import edu.nyu.cs6103.movietickets.shared.dto.AdminMovieRequest;
import edu.nyu.cs6103.movietickets.shared.dto.AdminTheaterRequest;
import edu.nyu.cs6103.movietickets.shared.dto.AdminShowtimeRequest;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Converts protocol requests into service calls and stable protocol responses. */
public final class RequestRouter {

    private static final System.Logger LOGGER =
            System.getLogger(RequestRouter.class.getName());

    private final JsonCodec codec;
    private final AuthenticationService authenticationService;
    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ShowtimeService showtimeService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final AdminService adminService;
    private final SeatLockManager seatLockManager;

    public RequestRouter(
            JsonCodec codec,
            AuthenticationService authenticationService,
            MovieService movieService,
            TheaterService theaterService,
            ShowtimeService showtimeService,
            SeatService seatService,
            BookingService bookingService,
            AdminService adminService) {
        this(codec, authenticationService, movieService, theaterService, showtimeService,
                seatService, bookingService, adminService, new SeatLockManager());
    }

    public RequestRouter(
            JsonCodec codec,
            AuthenticationService authenticationService,
            MovieService movieService,
            TheaterService theaterService,
            ShowtimeService showtimeService,
            SeatService seatService,
            BookingService bookingService,
            AdminService adminService,
            SeatLockManager seatLockManager) {
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
        this.authenticationService = Objects.requireNonNull(
                authenticationService, "authenticationService must not be null");
        this.movieService = Objects.requireNonNull(movieService, "movieService must not be null");
        this.theaterService = Objects.requireNonNull(theaterService, "theaterService must not be null");
        this.showtimeService = Objects.requireNonNull(showtimeService, "showtimeService must not be null");
        this.seatService = Objects.requireNonNull(seatService, "seatService must not be null");
        this.bookingService = Objects.requireNonNull(bookingService, "bookingService must not be null");
        this.adminService = Objects.requireNonNull(adminService, "adminService must not be null");
        this.seatLockManager = Objects.requireNonNull(seatLockManager, "seatLockManager must not be null");
    }

    public NetworkResponse handle(String json) {
        NetworkRequest request;
        try {
            request = codec.decodeRequest(json);
        } catch (RuntimeException exception) {
            return codec.error(
                    "invalid-" + UUID.randomUUID(),
                    new ErrorResponse("INVALID_REQUEST", "Invalid JSON request"));
        }

        try {
            return route(request);
        } catch (ValidationException | IllegalArgumentException exception) {
            return error(request, "INVALID_REQUEST", exception.getMessage());
        } catch (AuthenticationException exception) {
            return error(request, "AUTHENTICATION_REQUIRED", exception.getMessage());
        } catch (AuthorizationException exception) {
            return error(request, "AUTHORIZATION_DENIED", exception.getMessage());
        } catch (ResourceNotFoundException exception) {
            return error(request, "RESOURCE_NOT_FOUND", exception.getMessage(), Map.of(
                    "resourceType", exception.resourceType(),
                    "resourceId", String.valueOf(exception.resourceId())));
        } catch (SeatAlreadyBookedException exception) {
            return error(request, "SEAT_ALREADY_BOOKED",
                    "The selected seat is no longer available", Map.of(
                            "showtimeId", String.valueOf(exception.showtimeId()),
                            "seatId", String.valueOf(exception.seatId())));
        } catch (DatabaseOperationException | SQLException exception) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "Database request failed: " + request.requestId(), exception);
            return error(request, "DATABASE_ERROR", "The database operation failed");
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "Unexpected request failure: " + request.requestId(), exception);
            return error(request, "INTERNAL_ERROR", "An unexpected server error occurred");
        }
    }

    private NetworkResponse route(NetworkRequest request) throws SQLException {
        return switch (request.type()) {
            case REGISTER -> register(request);
            case LOGIN -> login(request);
            case LOGOUT -> logout(request);
            case GET_MOVIES -> getMovies(request);
            case GET_MOVIE -> getMovie(request);
            case GET_THEATERS -> getTheaters(request);
            case GET_THEATER -> getTheater(request);
            case GET_SHOWTIMES -> getShowtimes(request);
            case GET_SEAT_MAP -> getSeatMap(request);
            case LOCK_SEATS -> lockSeats(request);
            case RELEASE_SEATS -> releaseSeats(request);
            case CREATE_BOOKING -> createBooking(request);
            case CANCEL_BOOKING -> cancelBooking(request);
            case GET_BOOKING_HISTORY -> getBookingHistory(request);
            case ADMIN_CREATE_MOVIE -> adminCreateMovie(request);
            case ADMIN_UPDATE_MOVIE -> adminUpdateMovie(request);
            case ADMIN_CREATE_THEATER -> adminCreateTheater(request);
            case ADMIN_UPDATE_THEATER -> adminUpdateTheater(request);
            case ADMIN_CREATE_SHOWTIME -> adminCreateShowtime(request);
            case ADMIN_UPDATE_SHOWTIME -> adminUpdateShowtime(request);
        };
    }

    private NetworkResponse register(NetworkRequest request) throws SQLException {
        RegisterRequest data = codec.requestDataAs(request, RegisterRequest.class);
        User user = authenticationService.register(data.username(), data.password());
        return codec.success(request.requestId(),
                new RegisterResponse(user.id(), user.username()));
    }

    private NetworkResponse login(NetworkRequest request) throws SQLException {
        LoginRequest data = codec.requestDataAs(request, LoginRequest.class);
        AuthenticationService.LoginResult result =
                authenticationService.login(data.username(), data.password());
        User user = result.user();
        return codec.success(request.requestId(), new LoginResponse(
                user.id(), user.username(), user.role().toDatabaseValue(), result.token()));
    }

    private NetworkResponse logout(NetworkRequest request) {
        codec.requestDataAs(request, LogoutRequest.class);
        authenticationService.authenticate(request.token());
        authenticationService.logout(request.token());
        return codec.success(request.requestId(), Map.of("loggedOut", true));
    }

    private NetworkResponse getMovies(NetworkRequest request) {
        User user = requireUser(request);
        List<MovieDto> movies = (user.role().toDatabaseValue().equals("ADMIN")
                ? movieService.getAllMovies() : movieService.getAvailableMovies()).stream()
                .map(RequestRouter::movieDto).toList();
        return codec.success(request.requestId(), new MovieListResponse(movies));
    }

    private NetworkResponse getMovie(NetworkRequest request) {
        requireUser(request);
        MovieRequest data = codec.requestDataAs(request, MovieRequest.class);
        return codec.success(request.requestId(), movieDto(movieService.getMovie(data.movieId())));
    }

    private NetworkResponse getTheaters(NetworkRequest request) {
        requireUser(request);
        TheaterDto[] theaters = theaterService.getAllTheaters().stream()
                .map(RequestRouter::theaterDto).toArray(TheaterDto[]::new);
        return codec.success(request.requestId(), theaters);
    }

    private NetworkResponse getTheater(NetworkRequest request) {
        requireUser(request);
        TheaterRequest data = codec.requestDataAs(request, TheaterRequest.class);
        return codec.success(request.requestId(),
                theaterDto(theaterService.getTheater(data.theaterId())));
    }

    private NetworkResponse getShowtimes(NetworkRequest request) {
        requireUser(request);
        ShowtimeRequest data = codec.requestDataAs(request, ShowtimeRequest.class);
        List<ShowtimeDto> showtimes = showtimeService.getUpcomingShowtimes(data.movieId())
                .stream().map(RequestRouter::showtimeDto).toList();
        return codec.success(request.requestId(), new ShowtimeListResponse(showtimes));
    }

    private NetworkResponse getSeatMap(NetworkRequest request) {
        requireUser(request);
        SeatMapRequest data = codec.requestDataAs(request, SeatMapRequest.class);
        SeatService.SeatMap map = seatService.getSeatMap(data.showtimeId());
        List<SeatDto> seats = map.seats().stream().map(item -> new SeatDto(
                item.seat().id(), item.seat().rowLabel(), item.seat().seatNumber(),
                item.seat().accessible(),
                item.status().name().equals("AVAILABLE")
                        && seatLockManager.isLockedByOther(
                                request.token(), data.showtimeId(), item.seat().id())
                        ? "LOCKED" : item.status().name())).toList();
        long available = seats.stream().filter(seat -> seat.status().equals("AVAILABLE")).count();
        long booked = seats.size() - available;
        return codec.success(request.requestId(), new SeatMapResponse(
                showtimeDto(map.showtime()), seats, available, booked));
    }

    private NetworkResponse lockSeats(NetworkRequest request) {
        requireUser(request);
        SeatLockRequest data = codec.requestDataAs(request, SeatLockRequest.class);
        SeatService.SeatMap map = seatService.getSeatMap(data.showtimeId());
        Map<Long, String> statuses = map.seats().stream().collect(java.util.stream.Collectors.toMap(
                item -> item.seat().id(), item -> item.status().name()));
        for (long seatId : data.seatIds()) {
            if (!"AVAILABLE".equals(statuses.get(seatId))) {
                throw new SeatAlreadyBookedException(data.showtimeId(), seatId);
            }
        }
        return codec.success(request.requestId(), new SeatLockResponse(
                data.showtimeId(), data.seatIds(),
                seatLockManager.lock(request.token(), data.showtimeId(), data.seatIds())));
    }

    private NetworkResponse releaseSeats(NetworkRequest request) {
        requireUser(request);
        SeatLockRequest data = codec.requestDataAs(request, SeatLockRequest.class);
        seatLockManager.release(request.token(), data.showtimeId(), data.seatIds());
        return codec.success(request.requestId(), Map.of("released", true));
    }

    private NetworkResponse createBooking(NetworkRequest request) {
        User user = requireUser(request);
        CreateBookingRequest data = codec.requestDataAs(request, CreateBookingRequest.class);
        seatLockManager.assertNotLockedByOther(
                request.token(), data.showtimeId(), data.seatIds());
        try {
            BookingService.BookingDetails booking = bookingService.createBooking(
                    user.id(), data.showtimeId(), data.seatIds());
            return codec.success(request.requestId(), BookingResponse.single(bookingDto(booking)));
        } finally {
            seatLockManager.release(request.token(), data.showtimeId(), data.seatIds());
        }
    }

    private NetworkResponse cancelBooking(NetworkRequest request) {
        User user = requireUser(request);
        CancelBookingRequest data = codec.requestDataAs(request, CancelBookingRequest.class);
        BookingService.BookingDetails booking =
                bookingService.cancelBooking(user.id(), data.bookingId());
        return codec.success(request.requestId(), BookingResponse.single(bookingDto(booking)));
    }

    private NetworkResponse getBookingHistory(NetworkRequest request) {
        User user = requireUser(request);
        codec.requestDataAs(request, BookingHistoryRequest.class);
        List<BookingDto> history = bookingService.getBookingHistory(user.id()).stream()
                .map(RequestRouter::bookingDto).toList();
        return codec.success(request.requestId(), BookingResponse.history(history));
    }

    private NetworkResponse adminCreateMovie(NetworkRequest request) throws SQLException {
        authenticationService.requireAdmin(request.token());
        return codec.success(request.requestId(), movieDto(adminService.createMovie(
                codec.requestDataAs(request, AdminMovieRequest.class))));
    }
    private NetworkResponse adminUpdateMovie(NetworkRequest request) throws SQLException {
        authenticationService.requireAdmin(request.token());
        return codec.success(request.requestId(), movieDto(adminService.updateMovie(
                codec.requestDataAs(request, AdminMovieRequest.class))));
    }
    private NetworkResponse adminCreateTheater(NetworkRequest request) throws SQLException {
        authenticationService.requireAdmin(request.token());
        return codec.success(request.requestId(), theaterDto(adminService.createTheater(
                codec.requestDataAs(request, AdminTheaterRequest.class))));
    }
    private NetworkResponse adminUpdateTheater(NetworkRequest request) throws SQLException {
        authenticationService.requireAdmin(request.token());
        return codec.success(request.requestId(), theaterDto(adminService.updateTheater(
                codec.requestDataAs(request, AdminTheaterRequest.class))));
    }
    private NetworkResponse adminCreateShowtime(NetworkRequest request) throws SQLException {
        authenticationService.requireAdmin(request.token());
        return codec.success(request.requestId(), showtimeDto(adminService.createShowtime(
                codec.requestDataAs(request, AdminShowtimeRequest.class))));
    }
    private NetworkResponse adminUpdateShowtime(NetworkRequest request) throws SQLException {
        authenticationService.requireAdmin(request.token());
        return codec.success(request.requestId(), showtimeDto(adminService.updateShowtime(
                codec.requestDataAs(request, AdminShowtimeRequest.class))));
    }

    private User requireUser(NetworkRequest request) {
        return authenticationService.authenticate(request.token());
    }

    private NetworkResponse error(NetworkRequest request, String code, String message) {
        return error(request, code, message, Map.of());
    }

    private NetworkResponse error(
            NetworkRequest request, String code, String message, Map<String, String> details) {
        return codec.error(request.requestId(), new ErrorResponse(code, message, details));
    }

    private static MovieDto movieDto(Movie movie) {
        return new MovieDto(
                movie.id(), movie.title(), movie.durationMinutes(), movie.description(),
                movie.genre(), movie.posterPath(), movie.active());
    }

    private static TheaterDto theaterDto(Theater theater) {
        return new TheaterDto(theater.id(), theater.name(), theater.location());
    }

    private static ShowtimeDto showtimeDto(Showtime showtime) {
        return new ShowtimeDto(
                showtime.id(), showtime.movieId(), showtime.theaterId(),
                showtime.startTime(), showtime.price(), showtime.status().toDatabaseValue());
    }

    private static BookingDto bookingDto(BookingService.BookingDetails details) {
        return new BookingDto(
                details.booking().id(), details.booking().userId(),
                details.booking().showtimeId(), details.booking().status().toDatabaseValue(),
                details.booking().totalPrice(), details.booking().createdAt(),
                details.booking().cancelledAt(),
                details.seats().stream().map(BookingSeat::seatId).toList());
    }
}
