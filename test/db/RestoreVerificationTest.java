package db;

import java.sql.*;
import java.util.Arrays;

/** Proves that a real isolated MySQL restore retained rows, triggers and readable data. */
public final class RestoreVerificationTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static long count(Connection con, String table) throws SQLException {
        try (Statement statement = con.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            require(rows.next(), "No count available");
            return rows.getLong(1);
        }
    }

    public static void main(String[] args) throws Exception {
        String original = System.getenv("HOSPITAL_DB_URL");
        String restored = System.getenv("HOSPITAL_TEST_RESTORE_DB_URL");
        String user = System.getenv("HOSPITAL_DB_USER");
        String password = System.getenv("HOSPITAL_DB_PASSWORD");
        try (Connection before = DriverManager.getConnection(original, user, password);
             Connection copy = DriverManager.getConnection(restored, user, password)) {
            for (String table : Arrays.asList("Patient", "Doctor", "Appointment",
                    "Medical_Records", "Audit_Event", "Nurse", "Room", "Department")) {
                require(count(before, table) == count(copy, table),
                        "Restore row-count mismatch for " + table);
            }
            require(count(copy, "Medical_Records") > 0, "Restore missing clinical fixture");
            try (PreparedStatement test = copy.prepareStatement(
                    "SELECT Prescription FROM Medical_Records LIMIT 1");
                 ResultSet row = test.executeQuery()) {
                require(row.next(), "Clinical record absent");
                require("Synthetic prescription".equals(
                        SecurityManager.decryptMedicalText(row.getString(1))),
                        "Restored ciphertext cannot be decrypted using retained key");
            }
            try (PreparedStatement triggers = copy.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=?")) {
                triggers.setString(1, copy.getCatalog());
                try (ResultSet r = triggers.executeQuery()) {
                    require(r.next() && r.getInt(1) >= 5,
                            "Restore lost database audit triggers");
                }
            }
        }
        System.out.println("Isolated MySQL backup restored: rows, audit triggers and decryption verified");
    }
}
