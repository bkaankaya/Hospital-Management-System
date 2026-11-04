package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

public final class IntegrationSmokeTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        require(DatabaseManager.testConnection(), "CI MySQL connection unavailable");

        int doctorId = 1001001;
        int patientId = 1002002;
        Doctor doctor = new Doctor(doctorId, "Ada", "Lovelace", "Female",
                "Initial address", "1990-01-01", "Cardiology");
        Patient patient = new Patient(patientId, "Anna", "Testpatient",
                "F", "Initial address", "2000-01-01");

        DoctorDAO doctors = new DoctorDAO();
        PatientDAO patients = new PatientDAO();
        require(doctors.tryInsertDoctor(doctor), "Doctor insert failed");
        require(patients.tryInsertPatient(patient), "Patient insert failed");
        require(!doctors.tryInsertDoctor(doctor), "Duplicate doctor insert should fail");
        require(!patients.tryInsertPatient(patient), "Duplicate patient insert should fail");
        require(doctors.checkDoctorLogin(doctorId, "Ada", "Lovelace"), "Doctor lookup failed");
        require(patients.checkPatientLogin(patientId, "Anna", "Testpatient"), "Patient lookup failed");
        require(doctors.tryUpdateAddress(doctorId, "Updated doctor address"), "Doctor address not updated");
        require(patients.tryUpdateAddress(patientId, "Updated patient address"), "Patient address not updated");
        require(patients.patientNameLike("Anna") != null &&
                !patients.patientNameLike("Anna").isEmpty(), "Patient search returned nothing");

        String date = LocalDate.now().plusDays(1).toString();
        AppointmentDAO appointmentDao = new AppointmentDAO();
        List<String> before = appointmentDao.getTimeList(doctorId, date);
        require(before != null && before.contains("09:00:00"), "Expected available slot missing");
        Appointment appointment = new Appointment(date, "09:00:00", patientId, doctorId);
        require(appointmentDao.bookAppointment(appointment), "Booking insert failed");
        require(!appointmentDao.bookAppointment(appointment), "Duplicate booking should be rejected");
        List<String> after = appointmentDao.getTimeList(doctorId, date);
        require(after != null && !after.contains("09:00:00"), "Booked slot was not removed");
        List<String> ids = appointmentDao.getDocAppointments(doctorId);
        require(ids != null && ids.size() == 1, "Doctor appointment lookup failed");
        int appointmentId = Integer.parseInt(ids.get(0));
        require(!appointmentDao.cancelDoctorAppointment(appointmentId, doctorId + 1),
                "A different doctor canceled an appointment");
        require(appointmentDao.cancelDoctorAppointment(appointmentId, doctorId),
                "Doctor cancellation failed");

        // Regression: eight simultaneous requests for one slot must yield one durable booking.
        // Every other request must fail closed; no dependence on external API keys.
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(8);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<Boolean>> attempts = new java.util.ArrayList<>();
        try {
            for (int index = 0; index < 8; index++) {
                attempts.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(15, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new AssertionError("Concurrent booking start timed out");
                    }
                    return appointmentDao.bookAppointment(
                            new Appointment(date, "09:30:00", patientId, doctorId));
                }));
            }
            require(ready.await(15, java.util.concurrent.TimeUnit.SECONDS),
                    "Not all booking workers became ready");
            start.countDown();
            int accepted = 0;
            for (java.util.concurrent.Future<Boolean> attempt : attempts) {
                if (attempt.get(30, java.util.concurrent.TimeUnit.SECONDS)) accepted++;
            }
            require(accepted == 1, "Exactly one of eight conflicting bookings must succeed");
            java.util.List<String> concurrentIds = appointmentDao.getDocAppointments(doctorId);
            require(concurrentIds != null && concurrentIds.size() == 1,
                    "Exactly one concurrent booking must persist");
            require(appointmentDao.cancelDoctorAppointment(
                            Integer.parseInt(concurrentIds.get(0)), doctorId),
                    "Concurrent test appointment cleanup failed");
        } finally {
            start.countDown();
            pool.shutdownNow();
        }


        RoomDAO rooms = new RoomDAO();
        rooms.insertRoom(new Room(7001, "Cardiology"));
        rooms.updateRoomDepartment(new Room(7001, "Neurology"));
        require(rooms.getRoomList("Neurology") != null &&
                rooms.getRoomList("Neurology").contains("7001"), "Room update/query failed");

        MedicineDAO medicines = new MedicineDAO();
        medicines.insertMedicine(new Medicine(7002, "TestMedicine", "Cardiology"));
        require(medicines.getMedicineList("Cardiology") != null &&
                medicines.getMedicineList("Cardiology").contains("TestMedicine"), "Medicine query failed");
        List<String> stats = medicines.getStatistics("Cardiology");
        require(stats != null && stats.size() == 2 && stats.get(1) != null,
                "Medicine statistics failed");

        new MedicalRecordsDAO().add_medRecord(
                new MedicalRecords("Synthetic prescription", "Synthetic diagnosis", patientId),
                appointment);
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT Prescription, Diagnosis FROM Medical_Records WHERE Patient_SSN = ?")) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                require(rs.next(), "Medical record transaction failed");
                require(rs.getString(1).startsWith("enc:v1:"), "Prescription not encrypted");
                require("Synthetic prescription".equals(SecurityManager.decryptMedicalText(rs.getString(1))), "Medical decryption mismatch");
                require(rs.getString(2).startsWith("enc:v1:"), "Diagnosis not encrypted");
            }
        }
        System.out.println("Hospital MySQL integration smoke tests passed");
    }
}
