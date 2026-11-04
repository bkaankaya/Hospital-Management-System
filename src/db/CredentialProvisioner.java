package db;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * One-time, administrator-authorized console enrollment for pre-existing rows
 * with NULL password_hash. Does not overwrite already provisioned credentials.
 */
public final class CredentialProvisioner {
    private CredentialProvisioner() { }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run from a real terminal; credentials are never accepted as command arguments.");
            System.exit(2);
            return;
        }
        String administrator = System.getenv("HOSPITAL_ADMIN_PASSWORD");
        if (administrator == null || administrator.length() < 12) {
            System.err.println("Configure HOSPITAL_ADMIN_PASSWORD before enrollment.");
            System.exit(2);
            return;
        }
        char[] enteredAdmin = console.readPassword("Administrator password: ");
        try {
            byte[] enteredBytes = new String(enteredAdmin).getBytes(StandardCharsets.UTF_8);
            byte[] expectedBytes = administrator.getBytes(StandardCharsets.UTF_8);
            boolean authorized = MessageDigest.isEqual(enteredBytes, expectedBytes);
            Arrays.fill(enteredBytes, (byte) 0);
            Arrays.fill(expectedBytes, (byte) 0);
            if (!authorized) {
                System.err.println("Enrollment authorization failed.");
                System.exit(2);
                return;
            }
        } finally { Arrays.fill(enteredAdmin, '\0'); }

        String role = console.readLine("Role (patient/doctor): ").trim().toLowerCase();
        SecureAccounts.Kind kind;
        if ("patient".equals(role)) kind = SecureAccounts.Kind.PATIENT;
        else if ("doctor".equals(role)) kind = SecureAccounts.Kind.DOCTOR;
        else { System.err.println("Invalid role."); return; }
        int internalId;
        try { internalId = Integer.parseInt(console.readLine("Existing internal numeric ID: ").trim()); }
        catch (NumberFormatException ex) { System.err.println("Invalid internal ID."); return; }
        String nationalId = console.readLine("11-digit identifier: ").trim();
        if (!SecureAccounts.validNationalId(nationalId)) {
            System.err.println("Identifier must contain exactly 11 digits.");
            return;
        }
        char[] first = console.readPassword("New password (12+ characters): ");
        char[] second = console.readPassword("Confirm new password: ");
        try {
            if (!Arrays.equals(first, second)) {
                System.err.println("Passwords did not match.");
                return;
            }
            try {
                boolean provisioned = SecureAccounts.enrollExisting(kind, internalId, nationalId, first);
                System.out.println(provisioned
                        ? "Account successfully provisioned."
                        : "Provisioning failed or the account already has a password.");
            } catch (IllegalArgumentException ex) {
                System.err.println("Password requirements not met.");
            }
        } finally {
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
        }
    }
}
