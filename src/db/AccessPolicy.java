package db;

/**
 * Local desktop navigation rules. This is an in-process UI guard, not a substitute
 * for password authentication or database-side authorization.
 */
public final class AccessPolicy {
    public enum Role { NONE, PATIENT, DOCTOR, ADMIN }

    private AccessPolicy() {
    }

    public static boolean canOpen(Role role, String panelName) {
        if (panelName == null || role == null) return false;
        if (MainFrame.LOGIN_PANEL.equals(panelName)) return true;
        switch (role) {
            case PATIENT:
                return MainFrame.PATIENT_DASHBOARD_PANEL.equals(panelName)
                        || MainFrame.PATIENT_UPDATE_PANEL.equals(panelName)
                        || MainFrame.APPOINTMENT_PANEL.equals(panelName);
            case DOCTOR:
                return MainFrame.DOCTOR_DASHBOARD_PANEL.equals(panelName)
                        || MainFrame.DOCTOR_UPDATE_PANEL.equals(panelName)
                        || MainFrame.CANCEL_APPOINTMENT_PANEL.equals(panelName);
            case ADMIN:
                return MainFrame.INSERT_MENU_PANEL.equals(panelName)
                        || MainFrame.INSERT_DOCTOR_PANEL.equals(panelName)
                        || MainFrame.INSERT_PATIENT_PANEL.equals(panelName)
                        || MainFrame.PATIENT_NAME_LIKE_PANEL.equals(panelName)
                        || MainFrame.MEDICINE_STATS_PANEL.equals(panelName);
            case NONE:
            default:
                return false;
        }
    }
}
