package db;
/*
 * MedicalRecordsDAO.java
 * Author: Eyup Celen
 * This class is not used in GUI applications
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MedicalRecordsDAO {
    // Both inserts use the same connection and transaction. Never use MAX(record_no).
    public void add_medRecord(MedicalRecords record, Appointment appointment) throws SQLException {
        if (record == null || appointment == null) {
            throw new IllegalArgumentException("Record and appointment must not be null");
        }
        String recordSql = "INSERT INTO Medical_Records (Prescription, Diagnosis, Patient_SSN) VALUES (?, ?, ?)";
        String linkSql = "INSERT INTO `Add` (Doctor_SSN, Date, Time, Record_No) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseManager.getConnection()) {
            boolean originalAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int recordId;
                try (PreparedStatement insert = con.prepareStatement(recordSql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, record.Prescription);
                    insert.setString(2, record.Diagnosis);
                    insert.setInt(3, record.Patient_SSN);
                    if (insert.executeUpdate() != 1) {
                        throw new SQLException("Medical record insert affected no rows");
                    }
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Generated record ID missing");
                        recordId = keys.getInt(1);
                    }
                }
                try (PreparedStatement link = con.prepareStatement(linkSql)) {
                    link.setInt(1, appointment.Doctor_SSN);
                    link.setString(2, appointment.Date);
                    link.setString(3, appointment.Time);
                    link.setInt(4, recordId);
                    if (link.executeUpdate() != 1) {
                        throw new SQLException("Medical record linkage affected no rows");
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        }
    }
}
