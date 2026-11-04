package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

/**
 * Credential-based access to legacy patient and doctor tables.
 * The old numeric SSN is the internal foreign key, NOT a national identifier.
 * Unknown, inactive, unprovisioned and locked accounts all fail closed.
 */
public final class SecureAccounts {
    public enum Kind { PATIENT, DOCTOR }
    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 15;
    private static final String DUMMY_SALT = "MDEyMzQ1Njc4OWFiY2RlZg==";
    private static final String DUMMY_HASH =
            SecurityManager.hashPassword("unused-dummy-password".toCharArray(), DUMMY_SALT);

    private SecureAccounts() { }

    private static String table(Kind kind) {
        if (kind == Kind.PATIENT) return "Patient";
        if (kind == Kind.DOCTOR) return "Doctor";
        throw new IllegalArgumentException("Invalid account role");
    }

    public static boolean validNationalId(String id) {
        // Format validation only, not proof of legal identity.
        return SecurityManager.isValidSSN(id);
    }

    private static boolean validLookup(String id) {
        return id != null && id.matches("[0-9]{1,11}") && !id.startsWith("-")
                && (id.length() == 11 || parseLegacyId(id) > 0);
    }

    private static int parseLegacyId(String value) {
        try {
            int number = Integer.parseInt(value);
            return number > 0 ? number : 0;
        } catch (NumberFormatException ex) { return 0; }
    }

    /** Returns an existing internal FK identifier only after password verification. */
    public static int authenticate(Kind kind, String loginId, char[] password) {
        String sqlTable = table(kind);
        if (!validLookup(loginId) || password == null || password.length == 0) return 0;
        String sql = "SELECT SSN, password_hash, salt, is_active, Failed_Attempts, Locked_Until "
                + "FROM " + sqlTable + " WHERE National_ID = ? OR "
                + (loginId.length() == 11 ? "1 = 0" : "SSN = ?")
                + " LIMIT 1 FOR UPDATE";
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                int internalId = 0;
                String storedHash = null, storedSalt = null;
                boolean active = false;
                int failed = 0;
                Timestamp lockedUntil = null;
                try (PreparedStatement query = con.prepareStatement(sql)) {
                    query.setString(1, loginId);
                    if (loginId.length() != 11) query.setInt(2, parseLegacyId(loginId));
                    try (ResultSet found = query.executeQuery()) {
                        if (found.next()) {
                            internalId = found.getInt("SSN");
                            storedHash = found.getString("password_hash");
                            storedSalt = found.getString("salt");
                            active = found.getBoolean("is_active");
                            failed = found.getInt("Failed_Attempts");
                            lockedUntil = found.getTimestamp("Locked_Until");
                        }
                    }
                }
                // Always do a slow PBKDF2 verification, including unknown identities.
                boolean correct = SecurityManager.verifyPassword(password,
                        storedHash == null ? DUMMY_HASH : storedHash,
                        storedSalt == null ? DUMMY_SALT : storedSalt);
                boolean locked = lockedUntil != null && lockedUntil.toInstant().isAfter(Instant.now());
                if (internalId == 0 || !active || storedHash == null || locked || !correct) {
                    if (internalId > 0 && active && storedHash != null && !locked) {
                        int attempts = Math.min(MAX_FAILURES, failed + 1);
                        Timestamp newLock = attempts >= MAX_FAILURES
                                ? Timestamp.from(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES)) : null;
                        updateAttempts(con, sqlTable, internalId, attempts, newLock);
                    }
                    con.commit();
                    return 0;
                }
                updateAttempts(con, sqlTable, internalId, 0, null);
                con.commit();
                return internalId;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                return 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    private static void updateAttempts(Connection con, String table, int internalId,
                                       int failed, Timestamp lockedUntil) throws SQLException {
        try (PreparedStatement update = con.prepareStatement("UPDATE " + table
                + " SET Failed_Attempts = ?, Locked_Until = ? WHERE SSN = ?")) {
            update.setInt(1, failed);
            update.setTimestamp(2, lockedUntil);
            update.setInt(3, internalId);
            update.executeUpdate();
        }
    }

    /** Admin GUI calls after administrator login; requires a strong password. */
    public static boolean registerPatient(Patient patient, String nationalId, char[] password) {
        if (patient == null || patient.SSN <= 0 || !validNationalId(nationalId)) return false;
        SecurityManager.requireStrongPassword(password);
        String salt = SecurityManager.generateSalt();
        String hash = SecurityManager.hashPassword(password, salt);
        String sql = "INSERT INTO Patient (SSN, Name, Surname, Gender, Address, Date_Of_Birth, "
                + "National_ID, password_hash, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement insert = con.prepareStatement(sql)) {
            insert.setInt(1, patient.SSN);
            insert.setString(2, patient.Name);
            insert.setString(3, patient.Surname);
            insert.setString(4, patient.Gender);
            insert.setString(5, patient.Address);
            insert.setString(6, patient.Date_Of_Birth);
            insert.setString(7, nationalId);
            insert.setString(8, hash);
            insert.setString(9, salt);
            return insert.executeUpdate() == 1;
        } catch (SQLException ex) { return false; }
    }

    public static boolean registerDoctor(Doctor doctor, String nationalId, char[] password) {
        if (doctor == null || !validNationalId(nationalId)) return false;
        SecurityManager.requireStrongPassword(password);
        String salt = SecurityManager.generateSalt();
        String hash = SecurityManager.hashPassword(password, salt);
        String sql = "INSERT INTO Doctor (SSN, Name, Surname, Gender, Address, Date_Of_Birth, "
                + "Department_Name, National_ID, password_hash, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement insert = con.prepareStatement(sql)) {
            insert.setInt(1, doctor.getSSN());
            insert.setString(2, doctor.getName());
            insert.setString(3, doctor.getSurname());
            insert.setString(4, doctor.getGender());
            insert.setString(5, doctor.getAddress());
            insert.setString(6, doctor.getDateOfBirthAsString());
            insert.setString(7, doctor.getDepartmentName());
            insert.setString(8, nationalId);
            insert.setString(9, hash);
            insert.setString(10, salt);
            return insert.executeUpdate() == 1;
        } catch (SQLException ex) { return false; }
    }

    /**
     * Explicit credential enrollment for existing accounts. A previously set password
     * is never overwritten by this bootstrap command.
     */
    static boolean enrollExisting(Kind kind, int internalId, String nationalId, char[] password) {
        if (internalId <= 0 || !validNationalId(nationalId)) return false;
        SecurityManager.requireStrongPassword(password);
        String salt = SecurityManager.generateSalt();
        String hash = SecurityManager.hashPassword(password, salt);
        String sql = "UPDATE " + table(kind) + " SET National_ID=?, password_hash=?, salt=?, "
                + "Failed_Attempts=0, Locked_Until=NULL WHERE SSN=? AND password_hash IS NULL";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nationalId);
            stmt.setString(2, hash);
            stmt.setString(3, salt);
            stmt.setInt(4, internalId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) { return false; }
    }
}
