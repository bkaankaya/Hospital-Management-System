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
