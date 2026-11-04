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

        Room room = new Room(100, "Cardiology");
        require(room.Room_ID == 100, "Room model construction failed");
        System.out.println("Hospital smoke tests passed");
    }
}
