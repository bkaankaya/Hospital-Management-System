package db;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Güvenlik işlemlerini yöneten sınıf
 * Şifreleme, hash işlemleri ve güvenli random değer üretimi
 */
public class SecurityManager {
    private static final Logger logger = Logger.getLogger(SecurityManager.class.getName());
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    /**
     * Şifreyi güvenli bir şekilde hash'ler (SHA-256 + Salt)
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            
            // Byte array'i hex string'e çevir
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedPassword) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.severe("Şifre hash'leme hatası: " + e.getMessage());
            throw new RuntimeException("Şifre işlemi başarısız", e);
        }
    }
    
    /**
     * Güvenli salt değeri üretir
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Hassas verileri şifreler (SSN, adres vb.)
     */
    public static String encryptSensitiveData(String data, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            logger.severe("Veri şifreleme hatası: " + e.getMessage());
            throw new RuntimeException("Şifreleme işlemi başarısız", e);
        }
    }
    
    /**
     * Şifreli verileri çözer
     */
    public static String decryptSensitiveData(String encryptedData, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData);
        } catch (Exception e) {
            logger.severe("Veri çözme hatası: " + e.getMessage());
            throw new RuntimeException("Çözme işlemi başarısız", e);
        }
    }
    
    /**
     * Girdi doğrulama - SQL Injection koruması
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        
        // Tehlikeli karakterleri temizle
        return input.replaceAll("[';\"\\\\]", "")
                   .trim();
    }
    
    /**
     * SSN formatını doğrular
     */
    public static boolean isValidSSN(String ssn) {
        return ssn != null && ssn.matches("\\d{11}");
    }
    
    /**
     * Email formatını doğrular
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
