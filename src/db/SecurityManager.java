package db;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Pure JDK password hashing and authenticated data encryption. */
public final class SecurityManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 600000;
    private static final String PREFIX = "pbkdf2-sha256$600000$";
    private static final String CIPHER_PREFIX = "enc:v1:";
    private SecurityManager() { }

    public static String generateSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String hashPassword(String password, String salt) {
        char[] chars = password.toCharArray();
        try { return hashPassword(chars, salt); }
        finally { Arrays.fill(chars, '\0'); }
    }

    public static String hashPassword(char[] password, String salt) {
        if (password == null || password.length == 0) throw new IllegalArgumentException("Missing password");
        byte[] rawSalt = Base64.getDecoder().decode(salt);
        if (rawSalt.length < 16) throw new IllegalArgumentException("Salt too short");
        PBEKeySpec spec = new PBEKeySpec(password, rawSalt, ITERATIONS, 256);
        try {
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            try { return PREFIX + Base64.getEncoder().encodeToString(derived); }
            finally { Arrays.fill(derived, (byte) 0); }
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(rawSalt, (byte) 0);
        }
    }

    public static boolean verifyPassword(char[] password, String hash, String salt) {
        if (password == null || password.length == 0 || hash == null || salt == null
                || !hash.startsWith(PREFIX)) return false;
        try {
            byte[] expected = Base64.getDecoder().decode(hash.substring(PREFIX.length()));
            byte[] actual = Base64.getDecoder().decode(hashPassword(password, salt).substring(PREFIX.length()));
            try { return MessageDigest.isEqual(expected, actual); }
            finally { Arrays.fill(expected, (byte) 0); Arrays.fill(actual, (byte) 0); }
        } catch (RuntimeException e) { return false; }
    }

    public static void requireStrongPassword(char[] password) {
        if (password == null || password.length < 12 || password.length > 128)
            throw new IllegalArgumentException("Password must be 12 to 128 characters");
        boolean letter = false, digit = false;
        for (char ch : password) {
            if (Character.isLetter(ch)) letter = true;
            if (Character.isDigit(ch)) digit = true;
            if (Character.isISOControl(ch)) throw new IllegalArgumentException("Control character in password");
        }
        if (!letter || !digit) throw new IllegalArgumentException("Use letters and numbers");
    }

    public static String encryptSensitiveData(String value, String key) {
        return value == null ? null : encrypt(value, key.getBytes(StandardCharsets.UTF_8));
    }

    public static String decryptSensitiveData(String value, String key) {
        return value == null ? null : decrypt(value, key.getBytes(StandardCharsets.UTF_8));
    }

    public static String encryptMedicalText(String value) {
        return value == null ? null : encrypt(value, medicalKey());
    }

    public static String decryptMedicalText(String value) {
        return value == null ? null : decrypt(value, medicalKey());
    }

    private static byte[] medicalKey() {
        String configured = System.getenv("HOSPITAL_DATA_KEY_BASE64");
        if (configured == null || configured.isBlank())
            throw new IllegalStateException("HOSPITAL_DATA_KEY_BASE64 is required for medical records");
        try {
            byte[] key = Base64.getDecoder().decode(configured);
            if (key.length != 32) throw new IllegalArgumentException();
            return key;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Expected a base64-encoded 32-byte medical encryption key");
        }
    }

    private static String encrypt(String value, byte[] key) {
        validate(key);
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] payload = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + payload.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(payload, 0, combined, iv.length, payload.length);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) { throw new IllegalStateException("Data encryption failed", e); }
    }

    private static String decrypt(String value, byte[] key) {
        validate(key);
        if (!value.startsWith(CIPHER_PREFIX)) throw new IllegalArgumentException("Legacy ciphertext requires migration");
        try {
            byte[] combined = Base64.getDecoder().decode(value.substring(CIPHER_PREFIX.length()));
            if (combined.length < 29) throw new IllegalArgumentException("Invalid ciphertext");
            byte[] iv = Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalArgumentException("Authentication tag or ciphertext invalid", e); }
    }

    private static void validate(byte[] key) {
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32))
            throw new IllegalArgumentException("Invalid AES key length");
    }

    /** Always use JDBC PreparedStatement for database operations. */
    public static String sanitizeInput(String input) {
        return input == null ? "" : input.trim();
    }

    public static boolean isValidSSN(String value) {
        return value != null && value.matches("[0-9]{11}");
    }

    public static boolean isValidEmail(String value) {
        return value != null && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
