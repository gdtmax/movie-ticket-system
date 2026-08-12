package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.exception.AuthenticationException;
import edu.nyu.cs6103.movietickets.server.exception.AuthorizationException;
import edu.nyu.cs6103.movietickets.server.model.User;
import edu.nyu.cs6103.movietickets.server.security.TokenGenerator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe in-memory session storage for concurrent client handlers. */
public final class SessionService {

    public static final Duration DEFAULT_SESSION_LIFETIME = Duration.ofHours(8);

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final TokenGenerator tokenGenerator;
    private final Duration sessionLifetime;
    private final Clock clock;

    public SessionService(TokenGenerator tokenGenerator) {
        this(tokenGenerator, DEFAULT_SESSION_LIFETIME, Clock.systemUTC());
    }

    public SessionService(TokenGenerator tokenGenerator, Duration sessionLifetime, Clock clock) {
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator must not be null");
        this.sessionLifetime = Objects.requireNonNull(sessionLifetime, "sessionLifetime must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        if (sessionLifetime.isZero() || sessionLifetime.isNegative()) {
            throw new IllegalArgumentException("sessionLifetime must be positive");
        }
    }

    public String createSession(User user) {
        Objects.requireNonNull(user, "user must not be null");
        Instant createdAt = clock.instant();
        Session session = new Session(user, createdAt, createdAt.plus(sessionLifetime));

        String token;
        do {
            token = tokenGenerator.generate();
        } while (sessions.putIfAbsent(token, session) != null);
        return token;
    }

    public User requireUser(String token) {
        String normalizedToken = requireToken(token);
        Session session = sessions.get(normalizedToken);
        if (session == null) {
            throw new AuthenticationException("Invalid or expired session");
        }
        if (!clock.instant().isBefore(session.expiresAt())) {
            sessions.remove(normalizedToken, session);
            throw new AuthenticationException("Invalid or expired session");
        }
        return session.user();
    }

    public User requireAdmin(String token) {
        User user = requireUser(token);
        if (!user.isAdmin()) {
            throw new AuthorizationException("Administrator access is required");
        }
        return user;
    }

    public boolean logout(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return sessions.remove(token.trim()) != null;
    }

    public int invalidateUserSessions(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be greater than zero");
        }
        int before = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().user().id() == userId);
        return before - sessions.size();
    }

    public int activeSessionCount() {
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        return sessions.size();
    }

    private static String requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Authentication is required");
        }
        return token.trim();
    }

    private record Session(User user, Instant createdAt, Instant expiresAt) {
    }
}
