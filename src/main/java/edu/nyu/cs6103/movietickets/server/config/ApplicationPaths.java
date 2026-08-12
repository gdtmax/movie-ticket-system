package edu.nyu.cs6103.movietickets.server.config;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves one stable application root instead of relying on an arbitrary cwd. */
public final class ApplicationPaths {
    public static final String HOME_PROPERTY = "movie.tickets.home";

    private ApplicationPaths() { }

    public static Path projectRoot() {
        String configured = System.getProperty(HOME_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return validate(Path.of(configured));
        }
        Path fromWorkingDirectory = findRoot(Path.of(System.getProperty("user.dir", ".")));
        if (fromWorkingDirectory != null) return fromWorkingDirectory;
        try {
            Path codeLocation = Path.of(ApplicationPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path fromClasses = findRoot(codeLocation);
            if (fromClasses != null) return fromClasses;
        } catch (URISyntaxException | RuntimeException ignored) {
            // The final error below includes the supported explicit override.
        }
        throw new IllegalStateException("Cannot locate the project root. Start the server from "
                + "the directory containing pom.xml, or set -D" + HOME_PROPERTY + "=<project-path>");
    }

    public static String resolveDatabaseUrl(String databaseUrl, Path root) {
        String prefix = "jdbc:sqlite:";
        if (!databaseUrl.startsWith(prefix)) return databaseUrl;
        String target = databaseUrl.substring(prefix.length());
        if (target.equals(":memory:") || target.startsWith("file:") || Path.of(target).isAbsolute()) {
            return databaseUrl;
        }
        return prefix + root.resolve(target).normalize().toAbsolutePath();
    }

    private static Path findRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) current = current.getParent();
        for (int level = 0; current != null && level < 8; level++, current = current.getParent()) {
            if (isRoot(current)) return current;
        }
        return null;
    }

    private static Path validate(Path candidate) {
        Path root = candidate.toAbsolutePath().normalize();
        if (!isRoot(root)) throw new IllegalStateException("Invalid project root: " + root);
        return root;
    }

    private static boolean isRoot(Path path) {
        return Files.isRegularFile(path.resolve("pom.xml"))
                && Files.isRegularFile(path.resolve("database/schema.sql"))
                && Files.isRegularFile(path.resolve("database/seed.sql"));
    }
}
