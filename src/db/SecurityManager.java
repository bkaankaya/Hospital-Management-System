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

    /**
     * Versioned encryption envelope. The active key belongs to the trusted
     * server's protected keyring, never to the distributed Swing application.
     * Legacy v1 ciphertext is only readable with the separately retained old key.
     */
    public static String encryptMedicalText(String value) {
        if (value == null) return null;
        String active = System.getenv("HOSPITAL_DATA_ACTIVE_KEY_ID");
        if (active == null || active.isBlank()) {
            if (!Boolean.getBoolean("hospital.ci.legacyFixtures"))
                throw new IllegalStateException("An active medical encryption key is required");
            byte[] old = medicalKey();
            try { return encrypt(value, old); }
            finally { Arrays.fill(old, (byte) 0); }
        }
        byte[] key = keyFromKeyring(active);
        try {
            String legacyEnvelope = encrypt(value, key);
            return "enc:v2:" + active + ":" + legacyEnvelope.substring(CIPHER_PREFIX.length());
        } finally { Arrays.fill(key, (byte) 0); }
    }

    public static String decryptMedicalText(String value) {
        if (value == null) return null;
        if (value.startsWith("enc:v2:")) {
            int separator = value.indexOf(':', "enc:v2:".length());
            if (separator <= 7) throw new IllegalArgumentException("Invalid key reference");
            String id = value.substring(7, separator);
            byte[] key = keyFromKeyring(id);
            try { return decrypt(CIPHER_PREFIX + value.substring(separator + 1), key); }
            finally { Arrays.fill(key, (byte) 0); }
        }
        if (value.startsWith(CIPHER_PREFIX)) {
            byte[] old = medicalKey();
            try { return decrypt(value, old); }
            finally { Arrays.fill(old, (byte) 0); }
        }
        throw new IllegalArgumentException("Unsupported or unencrypted data format");
    }

    /** Designed for backed-up and explicitly approved in-place migrations. */
    public static String reencryptMedicalText(String value) {
        if (value == null) return null;
        String active = activeKeyId();
        if (value.startsWith("enc:v2:" + active + ":")) return value;
        if (value.startsWith("enc:") && !value.startsWith("enc:v2:")
                && !value.startsWith(CIPHER_PREFIX)) {
            throw new IllegalArgumentException("Unknown ciphertext format");
        }
        String plain = value.startsWith("enc:") ? decryptMedicalText(value) : value;
        return encryptMedicalText(plain);
    }

    public static boolean isEncryptedWithActiveKey(String value) {
        return value == null || value.startsWith("enc:v2:" + activeKeyId() + ":");
    }

    private static String activeKeyId() {
        String id = System.getenv("HOSPITAL_DATA_ACTIVE_KEY_ID");
        if (!validKeyId(id)) throw new IllegalStateException("Active key ID is missing or invalid");
        return id;
    }

    private static boolean validKeyId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{1,32}");
    }

    private static byte[] keyFromKeyring(String id) {
        if (!validKeyId(id)) throw new IllegalArgumentException("Invalid key identifier");
        String directory = System.getenv("HOSPITAL_DATA_KEYRING_DIR");
        if (directory == null || directory.isBlank())
            throw new IllegalStateException("HOSPITAL_DATA_KEYRING_DIR must be configured");
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(directory).toRealPath();
            java.nio.file.Path file = dir.resolve(id + ".key");
            if (!java.nio.file.Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    || java.nio.file.Files.isSymbolicLink(file))
                throw new IllegalStateException("Medical key is absent or not a regular file");
            if (java.nio.file.Files.getFileStore(file).supportsFileAttributeView("posix")) {
                java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions =
                        java.nio.file.Files.getPosixFilePermissions(file);
                for (java.nio.file.attribute.PosixFilePermission p : permissions) {
                    if (p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_"))
                        throw new IllegalStateException("Medical key file permissions must be owner-only");
                }
            }
            String base64 = java.nio.file.Files.readString(file, StandardCharsets.US_ASCII).trim();
            byte[] key = Base64.getDecoder().decode(base64);
            if (key.length != 32) throw new IllegalStateException("Medical key must be 256 bits");
            return key;
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Cannot access a protected medical encryption key", ex);
        }
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
