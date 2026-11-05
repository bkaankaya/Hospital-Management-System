package db;

import java.sql.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;

/** Synthetic-only proof of migrated ciphertext and backup-digest rejection. */
public final class MigrationVerificationTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("[A-Za-z0-9_-]{1,32}"))
            throw new IllegalArgumentException("Supply the expected active key ID");
        String prefix = "enc:v2:" + args[0] + ":";
        try (Connection connection = DatabaseManager.getConnection()) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT Prescription, Diagnosis FROM Medical_Records")) {
                int count = 0;
                while (rs.next()) {
                    String prescription = rs.getString(1);
                    String diagnosis = rs.getString(2);
                    check(prescription.startsWith(prefix) && diagnosis.startsWith(prefix),
                            "Clinical row has unencrypted or stale-key data");
                    check("Synthetic prescription".equals(
                            SecurityManager.decryptMedicalText(prescription)),
                            "Prescription cannot be decrypted after migration/rotation");
                    check("Synthetic diagnosis".equals(
                            SecurityManager.decryptMedicalText(diagnosis)),
                            "Diagnosis cannot be decrypted after migration/rotation");
                    count++;
                }
                check(count > 0, "Clinical test record missing");
            }
            for (String table : Arrays.asList("Patient", "Doctor", "Nurse")) {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT Address, Address_Cipher FROM " + table)) {
                    while (rs.next()) {
                        check(rs.getString(1) == null,
                                "Unencrypted address remained after migration: " + table);
                        String cipher = rs.getString(2);
                        if (cipher != null) {
                            check(cipher.startsWith(prefix),
                                    "Address has stale encryption key: " + table);
                            check(!SecurityManager.decryptMedicalText(cipher).isEmpty(),
                                    "Address decryption returned no text");
                        }
                    }
                }
            }
        }

        Path backup = Files.createTempFile("hospital-digest-fixture", ".bin");
        try {
            byte[] bytes = new byte[128];
            new java.security.SecureRandom().nextBytes(bytes);
            Files.write(backup, bytes);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] valid = digest.digest(bytes);
            StringBuilder expected = new StringBuilder();
            for (byte b : valid) expected.append(String.format("%02x", b & 0xff));
            VerifiedBackup.verify(backup, expected.toString());
            Files.writeString(backup, "tampered");
            try {
                VerifiedBackup.verify(backup, expected.toString());
                throw new AssertionError("Backup tampering not detected");
            } catch (IllegalArgumentException ignored) { }
        } finally { Files.deleteIfExists(backup); }
        System.out.println("Clinical text, addresses, active-key rotation and backup integrity verified");
    }
}
