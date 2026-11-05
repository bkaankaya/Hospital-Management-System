package db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Real TLS gateway/role tests over disposable MySQL and a synthetic keystore. */
public final class GatewayIntegrationTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void expectDenied(GatewayClient gateway, String operation, String... args) {
        try {
            gateway.call(operation, args);
            throw new AssertionError("Unauthorized operation succeeded: " + operation);
        } catch (IOException expected) { }
    }

    public static void main(String[] args) throws Exception {
        GatewayClient client = GatewayClient.get();
        check(GatewayClient.useGateway(), "Gateway was unexpectedly replaced by direct JDBC");
        expectDenied(client, "PATIENT_SCHEDULE");

        int patientId = client.login(GatewayPolicy.Role.PATIENT, "90000000001",
                "DemoSecurePassword123!".toCharArray());
        check(patientId == 1002002, "Patient session did not originate from server authentication");
        check(client.call("PATIENT_SCHEDULE").isEmpty(), "Unexpected appointment fixture");
        check(!client.call("DEPARTMENTS").isEmpty(), "Patient cannot access department choices");
        expectDenied(client, "DOCTOR_SCHEDULE");
        expectDenied(client, "DOCTOR_CANCEL", "1");
        expectDenied(client, "REGISTER_PATIENT");
        expectDenied(client, "PATIENT_SEARCH", "Anna");
        expectDenied(client, "MED_STATS", "Cardiology");
        client.logout();
        expectDenied(client, "PATIENT_SCHEDULE");

        int doctorId = client.login(GatewayPolicy.Role.DOCTOR, "90000000002",
                "DemoSecurePassword123!".toCharArray());
        check(doctorId == 1001001, "Doctor login did not return verified internal ID");
        check(client.call("DOCTOR_SCHEDULE").isEmpty(), "Unexpected doctor schedule fixture");
        expectDenied(client, "PATIENT_SCHEDULE");
        expectDenied(client, "BOOK", "1001001", "2026-12-12", "09:00:00");
        expectDenied(client, "PATIENT_SEARCH", "Anna");
        client.logout();

        int adminId = client.login(GatewayPolicy.Role.ADMIN, "admin",
                System.getenv("HOSPITAL_ADMIN_PASSWORD").toCharArray());
        check(adminId == 0, "Administrator identity unexpected");
        check(!client.call("DEPARTMENTS").isEmpty(), "Admin departments were unavailable");
        check(!client.call("PATIENT_SEARCH", "Anna").isEmpty(), "Admin read unavailable");
        expectDenied(client, "PATIENT_SCHEDULE");
        expectDenied(client, "DOCTOR_SCHEDULE");
        expectDenied(client, "BOOK", "1001001", "2026-12-12", "09:00:00");
        // Register two additional synthetic principals through the trusted server.
        check(GatewayClient.success(client.call("REGISTER_PATIENT",
                "1002011", "Nora", "Synthetic", "Female", "Fixture address",
                "2001-01-01", "90000000011", "DemoSecurePassword123!")),
                "Administrator could not register a password-protected patient");
        check(GatewayClient.success(client.call("REGISTER_DOCTOR",
                "1001011", "Owen", "Synthetic", "Male", "Fixture address",
                "1988-01-01", "Cardiology", "90000000012", "DemoSecurePassword123!")),
                "Administrator could not register a password-protected doctor");
        client.logout();

        int otherPatient = client.login(GatewayPolicy.Role.PATIENT, "90000000011",
                "DemoSecurePassword123!".toCharArray());
        check(otherPatient == 1002011, "Second patient identity was not verified");
        String tomorrow = java.time.LocalDate.now().plusDays(1).toString();
        check(GatewayClient.success(client.call("BOOK", "1001001", tomorrow, "10:00:00")),
                "Second patient's booking failed");
        List<String[]> ownVisits = client.call("PATIENT_SCHEDULE");
        check(ownVisits.size() == 1, "Second patient's scheduled visit not visible");
        String bookedId = ownVisits.get(0)[0];
        // Simulate a trusted server-side password change. The already issued
        // bearer token must immediately become invalid on the next request.
        try (Connection database = DriverManager.getConnection(
                System.getenv("HOSPITAL_DB_URL"),
                System.getenv("HOSPITAL_TEST_DB_USER"),
                System.getenv("HOSPITAL_TEST_DB_PASSWORD"))) {
            String originalHash;
            String salt;
            try (java.sql.PreparedStatement select = database.prepareStatement(
                    "SELECT password_hash, salt FROM Patient WHERE SSN=?")) {
                select.setInt(1, otherPatient);
                try (java.sql.ResultSet row = select.executeQuery()) {
                    check(row.next(), "Synthetic user missing");
                    originalHash = row.getString(1);
                    salt = row.getString(2);
                }
            }
            String replacement = SecurityManager.hashPassword(
                    "ChangedPasswordForTest123".toCharArray(), salt);
            try (java.sql.PreparedStatement update = database.prepareStatement(
                    "UPDATE Patient SET password_hash=? WHERE SSN=?")) {
                update.setString(1, replacement);
                update.setInt(2, otherPatient);
                check(update.executeUpdate() == 1, "Credential change fixture failed");
            }
            expectDenied(client, "PATIENT_SCHEDULE");
            try (java.sql.PreparedStatement reset = database.prepareStatement(
                    "UPDATE Patient SET password_hash=? WHERE SSN=?")) {
                reset.setString(1, originalHash);
                reset.setInt(2, otherPatient);
                check(reset.executeUpdate() == 1, "Fixture credential restore failed");
            }
        }
        client.logout();

        client.login(GatewayPolicy.Role.PATIENT, "90000000001",
                "DemoSecurePassword123!".toCharArray());
        check(client.call("PATIENT_SCHEDULE").isEmpty(),
                "Patient could read another patient's appointment");
        client.logout();

        client.login(GatewayPolicy.Role.DOCTOR, "90000000012",
                "DemoSecurePassword123!".toCharArray());
        check(client.call("DOCTOR_SCHEDULE").isEmpty(),
                "Doctor could read an unrelated physician's schedule");
        check(!GatewayClient.success(client.call("DOCTOR_CANCEL", bookedId)),
                "Doctor cancelled another physician's appointment");
        client.logout();

        client.login(GatewayPolicy.Role.DOCTOR, "90000000002",
                "DemoSecurePassword123!".toCharArray());
        check(client.call("DOCTOR_SCHEDULE").size() == 1,
                "Authorized doctor could not view booked visit");
        check(GatewayClient.success(client.call("DOCTOR_CANCEL", bookedId)),
                "Authorized doctor could not cancel own unlinked visit");
        client.logout();

        // This account is independent from the root account used to prepare the
        // isolated CI schema. It cannot edit the trigger-owned audit ledger.
        try (Connection con = DriverManager.getConnection(
                System.getenv("HOSPITAL_DB_URL"),
                System.getenv("HOSPITAL_TEST_DB_USER"),
                System.getenv("HOSPITAL_TEST_DB_PASSWORD"));
             Statement statement = con.createStatement()) {
            statement.executeQuery("SELECT COUNT(*) FROM Audit_Event").close();
            try {
                statement.executeUpdate("DELETE FROM Audit_Event WHERE 1=0");
                throw new AssertionError("Gateway's database identity can modify audit history");
            } catch (SQLException expected) { }
        }
        System.out.println("Verified TLS server, server-side roles, row scope, token revocation and audit permissions");
    }
}
