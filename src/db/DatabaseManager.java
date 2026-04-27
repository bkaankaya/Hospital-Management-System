package db;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Temel veritabanı yöneticisi - Standart JDBC
 */
public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    
    // Singleton pattern ile thread-safe implementasyon
    private DatabaseManager() {
    }
    
    /**
     * Veritabanı bağlantısı oluşturur
     */
    public static Connection getConnection() throws SQLException {
        try {
            Properties dbProps = loadDatabaseProperties();
            
            String url = dbProps.getProperty("url");
            if (url == null || url.trim().isEmpty()) {
                url = "jdbc:mysql://localhost:3306/hospital?useSSL=true&serverTimezone=UTC";
            }
            
            String username = dbProps.getProperty("username");
            String password = dbProps.getProperty("password");
            
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            logger.severe("Veritabanı bağlantı hatası: " + e.getMessage());
            throw new SQLException("Veritabanı başlatma başarısız", e);
        }
    }
    
    /**
     * Veritabanı özelliklerini güvenli şekilde yükler
     */
    private static Properties loadDatabaseProperties() {
        Properties props = new Properties();
        try (FileReader reader = new FileReader("src/db/login.properties")) {
            props.load(reader);
            return props;
        } catch (Exception e) {
            logger.severe("Login properties dosyası okunamadı: " + e.getMessage());
            throw new RuntimeException("Konfigürasyon hatası", e);
        }
    }
    
    /**
     * Bağlantı durumunu kontrol eder
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.warning("Bağlantı testi başarısız: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Uygulama kapanırken çağrılmalı (Geriye Dönük Uyumluluk için bırakıldı)
     */
    public static void shutdown() {
        logger.info("Veritabanı yöneticisi kapatıldı");
    }
    
    /**
     * Pool istatistiklerini döner (Geriye Dönük Uyumluluk)
     */
    public static String getPoolStats() {
        return "Standart JDBC Modu Kullanımda";
    }
}
