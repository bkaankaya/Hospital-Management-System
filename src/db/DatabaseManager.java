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
        if (!Boolean.getBoolean("hospital.gateway.server")
                && !Boolean.getBoolean("hospital.ci.legacyFixtures")) {
            throw new SQLException("Direct JDBC is disabled in desktop runtime; use the trusted gateway");
        }
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
        // A desktop client must not silently send credentials across the network.
        // Remote connections are opt-in and require MySQL host-certificate verification.
        validateJdbcUrl(url, "true".equalsIgnoreCase(System.getenv("HOSPITAL_ALLOW_REMOTE_DB")));
        DriverManager.setLoginTimeout(10);
        try {
            java.util.Properties connectionProperties = new java.util.Properties();
            connectionProperties.setProperty("user", username);
            connectionProperties.setProperty("password", password);
            connectionProperties.setProperty("connectTimeout", "10000");
            connectionProperties.setProperty("socketTimeout", "20000");
            return DriverManager.getConnection(url, connectionProperties);
        } catch (SQLException ex) {
            // Keep credentials and full connection strings out of diagnostic logs.
            logger.log(Level.WARNING, "MySQL connection unavailable (SQLState {0})", ex.getSQLState());
            throw ex;
        }
    }

    /**
     * Reject nonstandard JDBC URLs and remote traffic unless explicitly approved
     * and protected using MySQL Connector/J's sslMode=VERIFY_IDENTITY.
     * Localhost is permitted for the existing isolated integration fixture.
     */
    static void validateJdbcUrl(String jdbc, boolean allowRemote) throws SQLException {
        if (jdbc == null || !jdbc.startsWith("jdbc:mysql://")) {
            throw new SQLException("Only standard jdbc:mysql:// connections are supported");
        }
        final java.net.URI uri;
        try {
            uri = new java.net.URI(jdbc.substring("jdbc:".length()));
        } catch (java.net.URISyntaxException ex) {
            throw new SQLException("Invalid JDBC URL syntax");
        }
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null || uri.getFragment() != null
                || uri.getPath() == null || uri.getPath().length() < 2
                || uri.getRawAuthority() == null || uri.getRawAuthority().contains(",")) {
            throw new SQLException("A single explicit database host and schema are required");
        }
        boolean loopback = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host) || "::1".equals(host);
        if (loopback) return;
        if (!allowRemote) {
            throw new SQLException("Remote database access is disabled; configure localhost");
        }
        String query = uri.getRawQuery();
        boolean verified = false;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=", 2);
                if (entry.length != 2) continue;
                if (entry[0].equalsIgnoreCase("sslMode")
                        && entry[1].equalsIgnoreCase("VERIFY_IDENTITY")) verified = true;
                if ((entry[0].equalsIgnoreCase("useSSL") && entry[1].equalsIgnoreCase("false"))
                        || (entry[0].equalsIgnoreCase("trustServerCertificate")
                        && entry[1].equalsIgnoreCase("true"))
                        || (entry[0].equalsIgnoreCase("allowPublicKeyRetrieval")
                        && entry[1].equalsIgnoreCase("true"))) {
                    throw new SQLException("Insecure JDBC options are forbidden for remote hosts");
                }
            }
        }
        if (!verified) throw new SQLException("Remote MySQL requires sslMode=VERIFY_IDENTITY");
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
