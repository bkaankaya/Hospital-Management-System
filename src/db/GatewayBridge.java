package db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * One UI-facing facade: trusted gateway by default; direct DAO calls only for
 * disposable CI fixture runs explicitly launched with the CI test property.
 * Never falls back to JDBC after an HTTPS or authorization failure.
 */
public final class GatewayBridge {
    private GatewayBridge() { }

    private static List<String[]> ask(String op, String... fields) {
        try { return GatewayClient.get().call(op, fields); }
        catch (IOException ex) { return null; }
    }

    private static ArrayList<String> column(List<String[]> rows) {
        if (rows == null) return null;
        ArrayList<String> values = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length != 1) return null;
            values.add(row[0]);
        }
        return values;
    }

    public static ArrayList<String> departments() {
        return GatewayClient.useGateway() ? column(ask("DEPARTMENTS"))
                : new DepartmentDAO().getDepartmentList();
    }

    public static ArrayList<String> doctors(String department) {
        return GatewayClient.useGateway() ? column(ask("DOCTORS", department))
                : new DoctorDAO().getDoctorList(department);
    }

    public static int doctorId(String label) {
        if (!GatewayClient.useGateway()) return new DoctorDAO().getDoctorSsnByLabel(label);
        List<String[]> answer = ask("DOCTOR_ID", label);
        try { return answer == null || answer.size() != 1
                ? 0 : Integer.parseInt(answer.get(0)[0]); }
        catch (RuntimeException ex) { return 0; }
    }

    public static ArrayList<String> slots(int doctorId, String date) {
        return GatewayClient.useGateway()
                ? column(ask("SLOTS", Integer.toString(doctorId), date))
                : new AppointmentDAO().getTimeList(doctorId, date);
    }

    public static boolean book(Appointment app) {
        if (!GatewayClient.useGateway()) return new AppointmentDAO().bookAppointment(app);
        return GatewayClient.successOrFalse(ask("BOOK",
                Integer.toString(app.Doctor_SSN), app.Date, app.Time));
    }

    public static List<String[]> patientSchedule(int patientId) {
        return GatewayClient.useGateway() ? ask("PATIENT_SCHEDULE")
                : new AppointmentDAO().getPatientUpcomingAppointments(patientId);
    }

    public static List<String[]> doctorSchedule(int doctorId) {
        return GatewayClient.useGateway() ? ask("DOCTOR_SCHEDULE")
                : new AppointmentDAO().getDoctorUpcomingAppointments(doctorId);
    }

    public static boolean cancel(int appointmentId, int doctorId) {
        return GatewayClient.useGateway()
                ? GatewayClient.successOrFalse(ask("DOCTOR_CANCEL", Integer.toString(appointmentId)))
                : new AppointmentDAO().cancelDoctorAppointment(appointmentId, doctorId);
    }

    public static boolean updateAddress(boolean doctor, int userId, String address) {
        if (!GatewayClient.useGateway()) return doctor
                ? new DoctorDAO().tryUpdateAddress(userId, address)
                : new PatientDAO().tryUpdateAddress(userId, address);
        return GatewayClient.successOrFalse(ask("ADDRESS", address));
    }

    public static boolean registerPatient(Patient person, String nationalId, char[] password) {
        if (!GatewayClient.useGateway()) return SecureAccounts.registerPatient(person, nationalId, password);
        return GatewayClient.successOrFalse(ask("REGISTER_PATIENT",
                Integer.toString(person.SSN), person.Name, person.Surname,
                person.Gender, person.Address, person.Date_Of_Birth,
                nationalId, new String(password)));
    }

    public static boolean registerDoctor(Doctor doctor, String nationalId, char[] password) {
        if (!GatewayClient.useGateway()) return SecureAccounts.registerDoctor(doctor, nationalId, password);
        return GatewayClient.successOrFalse(ask("REGISTER_DOCTOR",
                Integer.toString(doctor.getSSN()), doctor.getName(), doctor.getSurname(),
                doctor.getGender(), doctor.getAddress(), doctor.getDateOfBirthAsString(),
                doctor.getDepartmentName(), nationalId, new String(password)));
    }

    public static List<ArrayList<String>> patientSearch(String name) {
        if (!GatewayClient.useGateway()) return new PatientDAO().patientNameLike(name);
        List<String[]> answer = ask("PATIENT_SEARCH", name);
        if (answer == null) return null;
        List<ArrayList<String>> rows = new ArrayList<>();
        for (String[] row : answer) {
            ArrayList<String> each = new ArrayList<>();
            java.util.Collections.addAll(each, row);
            rows.add(each);
        }
        return rows;
    }

    public static ArrayList<String> medicineStatistics(String department) {
        return GatewayClient.useGateway()
                ? column(ask("MED_STATS", department))
                : new MedicineDAO().getStatistics(department);
    }
}
