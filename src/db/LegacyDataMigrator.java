package db;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trusted-host-only, repeatable migration of historical medical prose and
 * demographic addresses. Never runs on application startup.
 *
 * --scan: counts rows requiring migration; does not modify data.
 * --apply: verifies a supplied backup digest and explicit restore approval,
 *          then performs bounded, optimistic, transactional updates.
 *
 * Protected demographic names and national IDs remain a separate deployment
 * concern and are NOT mislabeled encrypted by this tool.
 */
public final class LegacyDataMigrator {
    private static final int PAGE_SIZE = 50;
    private LegacyDataMigrator() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || (!"--scan".equals(args[0]) && !"--apply".equals(args[0]))) {
            System.err.println("Usage: db.LegacyDataMigrator --scan | --apply");
            System.exit(2);
            return;
        }
        System.setProperty("hospital.gateway.server", "true");
        boolean apply = "--apply".equals(args[0]);
        if (apply) {
            if (!"YES_BACKUP_RESTORED".equals(System.getenv("HOSPITAL_MIGRATION_APPROVAL")))
                throw new IllegalStateException("Restore verification approval is required");
            String filename = System.getenv("HOSPITAL_MIGRATION_BACKUP_FILE");
            if (filename == null || filename.isBlank())
                throw new IllegalStateException("HOSPITAL_MIGRATION_BACKUP_FILE is required");
            VerifiedBackup.verify(Path.of(filename),
                    System.getenv("HOSPITAL_MIGRATION_BACKUP_SHA256"));
            if (System.getenv("HOSPITAL_DATA_KEYRING_DIR") == null
                    || System.getenv("HOSPITAL_DATA_ACTIVE_KEY_ID") == null)
                throw new IllegalStateException("Versioned encryption keyring is required");
        }
        long records = migrateMedicalRecords(apply);
        long addresses = 0;
        for (String table : Arrays.asList("Patient", "Doctor", "Nurse")) {
            addresses += migrateAddress(table, apply);
        }
        // Counts are metadata only; never print identities, addresses or clinical text.
        System.out.println((apply ? "Updated" : "Would update") + " clinical rows: " + records);
        System.out.println((apply ? "Updated" : "Would update") + " address rows: " + addresses);
    }

    private static boolean needsRotation(String value) {
        if (value == null) return false;
        String active = System.getenv("HOSPITAL_DATA_ACTIVE_KEY_ID");
        // In scan mode with no active key, return a conservative count.
        return active == null || !value.startsWith("enc:v2:" + active + ":");
    }

    private static String encrypted(String value, boolean apply) {
        if (value == null) return null;
        return apply ? SecurityManager.reencryptMedicalText(value) : value;
    }

    private static long migrateMedicalRecords(boolean apply) throws SQLException {
        long changed = 0;
        int lastId = 0;
        while (true) {
            try (Connection con = DatabaseManager.getConnection()) {
                con.setAutoCommit(false);
                try {
                    List<MedicalRow> rows = new ArrayList<>();
                    try (PreparedStatement select = con.prepareStatement(
                            "SELECT Record_No, Prescription, Diagnosis FROM Medical_Records "
                                    + "WHERE Record_No>? ORDER BY Record_No LIMIT " + PAGE_SIZE)) {
                        select.setInt(1, lastId);
                        try (ResultSet rs = select.executeQuery()) {
                            while (rs.next()) rows.add(new MedicalRow(rs.getInt(1),
                                    rs.getString(2), rs.getString(3)));
                        }
                    }
                    if (rows.isEmpty()) { con.commit(); return changed; }
                    for (MedicalRow row : rows) {
                        lastId = row.id;
                        if (!needsRotation(row.prescription) && !needsRotation(row.diagnosis)) continue;
                        if (!apply) { changed++; continue; }
                        String newPrescription = encrypted(row.prescription, true);
                        String newDiagnosis = encrypted(row.diagnosis, true);
                        try (PreparedStatement update = con.prepareStatement(
                                "UPDATE Medical_Records SET Prescription=?, Diagnosis=? "
                                        + "WHERE Record_No=? AND Prescription <=> ? AND Diagnosis <=> ?")) {
                            update.setString(1, newPrescription);
                            update.setString(2, newDiagnosis);
                            update.setInt(3, row.id);
                            update.setString(4, row.prescription);
                            update.setString(5, row.diagnosis);
                            if (update.executeUpdate() != 1)
                                throw new SQLException("Concurrent modification blocked a medical data migration");
                        }
                        changed++;
                    }
                    con.commit();
                } catch (SQLException | RuntimeException e) {
                    con.rollback();
                    throw e;
                }
            }
        }
    }

    private static long migrateAddress(String table, boolean apply) throws SQLException {
        // SQL identifiers come only from the hardcoded allowlist above.
        long changed = 0;
        int lastId = 0;
        while (true) {
            try (Connection con = DatabaseManager.getConnection()) {
                con.setAutoCommit(false);
                try {
                    List<AddressRow> rows = new ArrayList<>();
                    try (PreparedStatement select = con.prepareStatement(
                            "SELECT SSN, Address, Address_Cipher FROM " + table
                                    + " WHERE SSN>? ORDER BY SSN LIMIT " + PAGE_SIZE)) {
                        select.setInt(1, lastId);
                        try (ResultSet rs = select.executeQuery()) {
                            while (rs.next()) rows.add(new AddressRow(rs.getInt(1),
                                    rs.getString(2), rs.getString(3)));
                        }
                    }
                    if (rows.isEmpty()) { con.commit(); return changed; }
                    for (AddressRow row : rows) {
                        lastId = row.id;
                        if (row.plain == null && !needsRotation(row.cipher)) continue;
                        if (row.plain != null && row.cipher != null)
                            throw new SQLException("Ambiguous address migration state; manual review required");
                        if (!apply) { changed++; continue; }
                        String cipher = row.plain != null
                                ? SecurityManager.encryptMedicalText(row.plain)
                                : encrypted(row.cipher, true);
                        try (PreparedStatement update = con.prepareStatement(
                                "UPDATE " + table + " SET Address=NULL, Address_Cipher=? "
                                        + "WHERE SSN=? AND Address <=> ? AND Address_Cipher <=> ?")) {
                            update.setString(1, cipher);
                            update.setInt(2, row.id);
                            update.setString(3, row.plain);
                            update.setString(4, row.cipher);
                            if (update.executeUpdate() != 1)
                                throw new SQLException("Concurrent modification blocked an address migration");
                        }
                        changed++;
                    }
                    con.commit();
                } catch (SQLException | RuntimeException e) {
                    con.rollback();
                    throw e;
                }
            }
        }
    }

    private static final class MedicalRow {
        final int id;
        final String prescription, diagnosis;
        MedicalRow(int id, String prescription, String diagnosis) {
            this.id = id; this.prescription = prescription; this.diagnosis = diagnosis;
        }
    }
    private static final class AddressRow {
        final int id;
        final String plain, cipher;
        AddressRow(int id, String plain, String cipher) {
            this.id = id; this.plain = plain; this.cipher = cipher;
        }
    }
}
