package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.UserDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.db.TransactionManager;
import edu.nyu.cs6103.movietickets.server.exception.AuthenticationException;
import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.User;
import edu.nyu.cs6103.movietickets.server.model.UserRole;
import edu.nyu.cs6103.movietickets.server.security.PasswordHasher;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AuthenticationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,30}");
    private static final int MINIMUM_PASSWORD_CHARACTERS = 8;
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final DatabaseManager databaseManager;
    private final TransactionManager transactionManager;
    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final SessionService sessionService;

    public AuthenticationService(
            DatabaseManager databaseManager,
            TransactionManager transactionManager,
            UserDao userDao,
            PasswordHasher passwordHasher,
            SessionService sessionService) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService must not be null");
    }

    public User register(String username, String password) throws SQLException {
        String normalizedUsername = validateUsername(username);
        validatePassword(password);
        String passwordHash = passwordHasher.hash(password);

        try {
            return transactionManager.execute(connection ->
                    userDao.insert(connection, normalizedUsername, passwordHash, UserRole.USER));
        } catch (DatabaseOperationException exception) {
            if (isUsernameConstraint(exception)) {
                throw new ValidationException("Username is already registered");
            }
            throw exception;
        }
    }

    public LoginResult login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        User user;
        try (Connection connection = databaseManager.getConnection()) {
            user = userDao.findByUsername(connection, username.trim())
                    .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));
        }

        if (!passwordHasher.verify(password, user.passwordHash())) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        return new LoginResult(user, sessionService.createSession(user));
    }

    public User authenticate(String token) {
        return sessionService.requireUser(token);
    }

    public User requireAdmin(String token) {
        return sessionService.requireAdmin(token);
    }

    public boolean logout(String token) {
        return sessionService.logout(token);
    }

    private static String validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new ValidationException(
                    "Username must contain 3-30 letters, numbers, dots, underscores, or hyphens");
        }
        return username.trim();
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_CHARACTERS) {
            throw new ValidationException("Password must contain at least 8 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new ValidationException("Password must not exceed 72 UTF-8 bytes");
        }
    }

    private static boolean isUsernameConstraint(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("users.username")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public record LoginResult(User user, String token) {
        public LoginResult {
            Objects.requireNonNull(user, "user must not be null");
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("token must not be blank");
            }
        }
    }
}
