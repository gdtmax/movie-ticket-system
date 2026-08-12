package edu.nyu.cs6103.movietickets.server.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {

    @Test
    void loadsTheDefaultApplicationConfiguration() {
        ServerConfig config = ServerConfig.load();

        assertEquals("localhost", config.host());
        assertEquals(5050, config.port());
        assertEquals(50, config.threadPoolSize());
        assertEquals("jdbc:sqlite:data/movie-tickets.db", config.databaseUrl());
        assertEquals(5000, config.socketConnectTimeoutMillis());
        assertEquals(10000, config.socketReadTimeoutMillis());
    }

    @Test
    void loadsTheIsolatedTestConfiguration() {
        ServerConfig production = ServerConfig.load();
        ServerConfig test = ServerConfig.load("application-test.properties");

        assertEquals("127.0.0.1", test.host());
        assertEquals(15050, test.port());
        assertEquals(4, test.threadPoolSize());
        assertTrue(test.databaseUrl().endsWith("movie-tickets-test.db"));
        assertNotEquals(production.databaseUrl(), test.databaseUrl());
    }

    @Test
    void rejectsAMissingConfigurationResource() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ServerConfig.load("missing.properties"));

        assertTrue(exception.getMessage().contains("missing.properties"));
    }

    @Test
    void rejectsANonNumericPort() {
        Properties properties = validProperties();
        properties.setProperty("server.port", "not-a-number");

        assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.fromProperties(properties));
    }

    @Test
    void rejectsAnInvalidPortRange() {
        Properties properties = validProperties();
        properties.setProperty("server.port", "70000");

        assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.fromProperties(properties));
    }

    @Test
    void rejectsNonPositiveThreadPoolSize() {
        Properties properties = validProperties();
        properties.setProperty("server.thread-pool-size", "0");

        assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.fromProperties(properties));
    }

    private static Properties validProperties() {
        Properties properties = new Properties();
        properties.setProperty("server.host", "localhost");
        properties.setProperty("server.port", "5050");
        properties.setProperty("server.thread-pool-size", "10");
        properties.setProperty("database.url", "jdbc:sqlite:data/test.db");
        properties.setProperty("socket.connect-timeout", "1000");
        properties.setProperty("socket.read-timeout", "2000");
        return properties;
    }
}
