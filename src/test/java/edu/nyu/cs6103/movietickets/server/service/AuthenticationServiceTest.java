package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.UserDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseInitializer;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.db.TransactionManager;
import edu.nyu.cs6103.movietickets.server.exception.AuthenticationException;
import edu.nyu.cs6103.movietickets.server.exception.AuthorizationException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import edu.nyu.cs6103.movietickets.server.model.User;
import edu.nyu.cs6103.movietickets.server.model.UserRole;
import edu.nyu.cs6103.movietickets.server.security.PasswordHasher;
import edu.nyu.cs6103.movietickets.server.security.TokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationServiceTest {

    @TempDir
    Path tempDirectory;

    private AuthenticationService authenticationService;
    private DatabaseManager databaseManager;
    private TransactionManager transactionManager;
    private UserDao userDao;
    private PasswordHasher passwordHasher;

    @BeforeEach
    void setUp() throws Exception {
        databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + tempDirectory.resolve("authentication.db"));
        new DatabaseInitializer(databaseManager).initialize(testResource("test-schema.sql"));
        transactionManager = new TransactionManager(databaseManager);
        userDao = new UserDao();
        passwordHasher = new PasswordHasher(4);
        authenticationService = new AuthenticationService(
                databaseManager, transactionManager, userDao, passwordHasher,
                new SessionService(new TokenGenerator()));
    }

    @Test
    void registersLogsInAuthenticatesAndLogsOut() throws Exception {
        User registered = authenticationService.register("alice", "password-123");
        assertFalse(registered.passwordHash().equals("password-123"));

        AuthenticationService.LoginResult login =
                authenticationService.login("ALICE", "password-123");
        assertEquals(registered.id(), login.user().id());
        assertEquals(registered.id(), authenticationService.authenticate(login.token()).id());

        authenticationService.logout(login.token());
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(login.token()));
    }

    @Test
    void rejectsDuplicateAndInvalidRegistration() throws Exception {
        authenticationService.register("alice", "password-123");
        assertThrows(ValidationException.class,
                () -> authenticationService.register("ALICE", "different-123"));
        assertThrows(ValidationException.class,
                () -> authenticationService.register("a", "password-123"));
        assertThrows(ValidationException.class,
                () -> authenticationService.register("valid-name", "short"));
    }

    @Test
    void loginFailureDoesNotRevealWhetherUsernameExists() throws Exception {
        authenticationService.register("alice", "password-123");

        AuthenticationException wrongPassword = assertThrows(
                AuthenticationException.class,
                () -> authenticationService.login("alice", "wrong-password"));
        AuthenticationException missingUser = assertThrows(
                AuthenticationException.class,
                () -> authenticationService.login("missing", "wrong-password"));

        assertEquals(wrongPassword.getMessage(), missingUser.getMessage());
    }

    @Test
    void enforcesAdministratorRoleFromTheServerSession() throws Exception {
        User normalUser = authenticationService.register("alice", "password-123");
        AuthenticationService.LoginResult normalLogin =
                authenticationService.login("alice", "password-123");
        assertThrows(AuthorizationException.class,
                () -> authenticationService.requireAdmin(normalLogin.token()));

        String adminHash = passwordHasher.hash("admin-password");
        User admin = transactionManager.execute(connection ->
                userDao.insert(connection, "administrator", adminHash, UserRole.ADMIN));
        AuthenticationService.LoginResult adminLogin =
                authenticationService.login("administrator", "admin-password");

        assertNotEquals(normalUser.id(), admin.id());
        assertEquals(admin.id(), authenticationService.requireAdmin(adminLogin.token()).id());
    }

    private static Path testResource(String name) throws Exception {
        return Path.of(AuthenticationServiceTest.class.getClassLoader().getResource(name).toURI());
    }
}
