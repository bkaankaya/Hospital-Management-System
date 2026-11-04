package db;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Existing JDBC/MySQL connection manager.
 * Environment variables work even if the app is launched outside the checkout.
 */
public final class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private DatabaseManager() { }

    public static Connection getConnection() throws SQLException {
        Properties properties = loadDatabaseProperties();
        String url = override("HOSPITAL_DB_URL", properties.getProperty("url"));
        String username = override("HOSPITAL_DB_USER",
                properties.getProperty("username", properties.getProperty("user")));
        String password = System.getenv("HOSPITAL_DB_PASSWORD");
        if (password == null) password = properties.getProperty("password");

        if (url == null || !url.startsWith("jdbc:mysql:")) {
            throw new SQLException("Configure a JDBC MySQL URL in HOSPITAL_DB_URL or src/db/login.properties");
        }
        if (username == null || username.trim().isEmpty() || password == null) {
            throw new SQLException("Configure MySQL username/password in environment variables or login.properties");
        }
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException ex) {
            // Keep credentials and full connection strings out of diagnostic logs.
            logger.log(Level.WARNING, "MySQL connection unavailable (SQLState {0})", ex.getSQLState());
            throw ex;
        }
    }

    private static String override(String name, String fallback) {
        String environment = System.getenv(name);
        return environment == null || environment.trim().isEmpty() ? fallback : environment;
    }

    private static Properties loadDatabaseProperties() {
        Properties properties = new Properties();
        Path file = Paths.get("src", "db", "login.properties");
        if (!Files.isRegularFile(file)) return properties;
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not load optional local MySQL configuration file");
        }
        return properties;
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return !connection.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    /** Backward-compatible hook for legacy callers. */
    public static void shutdown() {
        logger.info("JDBC connection manager closed");
    }

    public static String getPoolStats() {
        return "Standard JDBC mode (no connection pool)";
    }
}
