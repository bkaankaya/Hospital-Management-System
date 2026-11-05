package db;

/** Centralized, fail-closed gateway permissions. The client never chooses its role. */
public final class GatewayPolicy {
    public enum Role { PATIENT, DOCTOR, ADMIN }
    private GatewayPolicy() { }

    public static boolean allowed(Role role, String operation) {
        if (role == null || operation == null) return false;
        switch (role) {
            case PATIENT:
                return operation.equals("DEPARTMENTS") || operation.equals("DOCTORS")
                    || operation.equals("DOCTOR_ID") || operation.equals("SLOTS")
                    || operation.equals("BOOK") || operation.equals("PATIENT_SCHEDULE")
                    || operation.equals("ADDRESS");
            case DOCTOR:
                return operation.equals("DOCTOR_SCHEDULE") || operation.equals("DOCTOR_CANCEL")
                    || operation.equals("ADDRESS");
            case ADMIN:
                return operation.equals("DEPARTMENTS") || operation.equals("REGISTER_PATIENT")
                    || operation.equals("REGISTER_DOCTOR") || operation.equals("PATIENT_SEARCH")
                    || operation.equals("MED_STATS");
            default:
                return false;
        }
    }
}
