package db;

import javax.swing.*;
import java.awt.*;

/** Display-backed Java-only regression test; run under xvfb on CI. */
public final class GuiSmokeTest {
    private static void require(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            try {
                frame.setVisible(true);
                require(frame.getJMenuBar() != null, "Navigation menu missing");
                require(frame.getSize().width >= 800, "Main window too narrow");
                LoginPanel login = find(frame, LoginPanel.class);
                require(login != null && login.getRoleTabCount() == 3,
                        "Three login roles must remain visible");
                require(find(frame, PatientDashboardPanel.class) != null,
                        "Patient workspace missing");
                require(find(frame, DoctorDashboardPanel.class) != null,
                        "Doctor workspace missing");
                require(find(frame, InsertMenuPanel.class) != null,
                        "Admin workspace missing");

                frame.setCurrentPatientSsn(1002002);
                frame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
                require(Integer.valueOf(1002002).equals(frame.getCurrentPatientSsn()),
                        "Patient session not retained");
                frame.showPanel(MainFrame.LOGIN_PANEL);
                require(frame.getCurrentPatientSsn() == null,
                        "Signing out must clear patient session");

                frame.setCurrentDoctorSsn(1001001);
                frame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL);
                require(Integer.valueOf(1001001).equals(frame.getCurrentDoctorSsn()),
                        "Doctor session not retained");
                frame.showPanel(MainFrame.LOGIN_PANEL);
                require(frame.getCurrentDoctorSsn() == null,
                        "Signing out must clear doctor session");

                frame.setAdminAuthenticated();
                frame.showPanel(MainFrame.INSERT_MENU_PANEL);
                require(find(frame, InsertMenuPanel.class).isShowing(),
                        "Admin dashboard not displayed");
            } finally {
                frame.dispose();
            }
        });
        System.out.println("Hospital Java Swing GUI smoke tests passed");
    }

    private static <T extends Component> T find(Container root, Class<T> target) {
        for (Component component : root.getComponents()) {
            if (target.isInstance(component)) return target.cast(component);
            if (component instanceof Container) {
                T nested = find((Container) component, target);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
