package edu.nyu.cs6103.movietickets.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Immutable application configuration loaded from a classpath properties file.
 */
public record ServerConfig(
        String host,
        int port,
        int threadPoolSize,
        String databaseUrl,
        int socketConnectTimeoutMillis,
        int socketReadTimeoutMillis) {

    public static final String DEFAULT_RESOURCE = "application.properties";

    public ServerConfig {
        host = requireNonBlank(host, "server.host");
        databaseUrl = requireNonBlank(databaseUrl, "database.url");
        requireRange(port, 1, 65_535, "server.port");
        requirePositive(threadPoolSize, "server.thread-pool-size");
        requirePositive(socketConnectTimeoutMillis, "socket.connect-timeout");
        requirePositive(socketReadTimeoutMillis, "socket.read-timeout");
    }

    /**
     * Loads the normal application configuration.
     */
    public static ServerConfig load() {
        return load(DEFAULT_RESOURCE);
    }

    /**
     * Loads configuration from a classpath resource, allowing tests to use an
     * isolated configuration file.
     */
    public static ServerConfig load(String resourceName) {
        String normalizedName = requireNonBlank(resourceName, "resourceName");
        Properties properties = new Properties();

        ClassLoader classLoader = ServerConfig.class.getClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(normalizedName)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Configuration resource not found: " + normalizedName);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read configuration resource: " + normalizedName,
                    exception);
        }

        return fromProperties(properties);
    }

    /**
     * Converts a set of properties into validated configuration values.
     */
    public static ServerConfig fromProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties must not be null");

        return new ServerConfig(
                requiredProperty(properties, "server.host"),
                integerProperty(properties, "server.port"),
                integerProperty(properties, "server.thread-pool-size"),
                requiredProperty(properties, "database.url"),
                integerProperty(properties, "socket.connect-timeout"),
                integerProperty(properties, "socket.read-timeout"));
    }

    private static String requiredProperty(Properties properties, String key) {
        return requireNonBlank(properties.getProperty(key), key);
    }

    private static int integerProperty(Properties properties, String key) {
        String value = requiredProperty(properties, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Configuration property must be an integer: " + key,
                    exception);
        }
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }

    private static void requireRange(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimum + " and " + maximum);
        }
    }
}
