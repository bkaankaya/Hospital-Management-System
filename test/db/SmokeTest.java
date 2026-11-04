package db;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public final class SmokeTest {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        List<String> dates = new AppointmentDAO().getDateList();
        require(dates.size() == 14, "Expected a 14-day booking window");
        require(dates.get(0).equals(LocalDate.now().toString()), "First day must be today");
        require(new HashSet<>(dates).size() == 14, "Booking dates must be distinct");

        Doctor doctor = new Doctor(12345, "Ada", "Lovelace", "Female",
                                   "Test Street", "1990-01-01", "Cardiology");
        require(doctor.getSSN() == 12345, "Legacy doctor ID should be retained");
        require("F".equals(doctor.getGender()), "Gender normalization failed");

        String salt = SecurityManager.generateSalt();
        require(!salt.isEmpty(), "Missing password salt");
        String hash = SecurityManager.hashPassword("test-password", salt);
        require(hash.equals(SecurityManager.hashPassword("test-password", salt)),
                "Password hashing should be stable for the same salt");
        require(!hash.equals(SecurityManager.hashPassword("different", salt)),
                "Different passwords should hash differently");

        String key = "0123456789abcdef";
        String cipher = SecurityManager.encryptSensitiveData("smoke", key);
        require("smoke".equals(SecurityManager.decryptSensitiveData(cipher, key)),
                "Legacy encryption round trip failed");

        // Desktop navigation must never cross between patient/doctor/admin screens.
        require(AccessPolicy.canOpen(AccessPolicy.Role.NONE, MainFrame.LOGIN_PANEL),
                "Login must be accessible before authentication");
        require(!AccessPolicy.canOpen(AccessPolicy.Role.NONE, MainFrame.INSERT_MENU_PANEL),
                "Anonymous access to admin insert menu must be denied");
        require(AccessPolicy.canOpen(AccessPolicy.Role.PATIENT, MainFrame.APPOINTMENT_PANEL),
                "Patients must retain the appointment flow");
        require(!AccessPolicy.canOpen(AccessPolicy.Role.PATIENT, MainFrame.CANCEL_APPOINTMENT_PANEL),
                "Patients must not open doctor cancellation");
        require(!AccessPolicy.canOpen(AccessPolicy.Role.PATIENT, MainFrame.INSERT_PATIENT_PANEL),
                "Patients must not open admin creation");
        require(AccessPolicy.canOpen(AccessPolicy.Role.DOCTOR, MainFrame.DOCTOR_UPDATE_PANEL),
                "Doctors must retain address update");
        require(!AccessPolicy.canOpen(AccessPolicy.Role.DOCTOR, MainFrame.PATIENT_UPDATE_PANEL),
                "Doctors must not enter patient update");
        require(AccessPolicy.canOpen(AccessPolicy.Role.ADMIN, MainFrame.INSERT_MENU_PANEL),
                "Admins must reach the insert menu");
        require(AccessPolicy.canOpen(AccessPolicy.Role.ADMIN, MainFrame.MEDICINE_STATS_PANEL),
                "Admins must reach statistics");
        require(!AccessPolicy.canOpen(AccessPolicy.Role.ADMIN, MainFrame.PATIENT_DASHBOARD_PANEL),
                "Admins must not impersonate patients");
        require(!AccessPolicy.canOpen(AccessPolicy.Role.ADMIN, "UnknownPanel"),
                "Unknown panels must be denied");

        Room room = new Room(100, "Cardiology");
        require(room.Room_ID == 100, "Room model construction failed");
        System.out.println("Hospital smoke tests passed");
    }
}
