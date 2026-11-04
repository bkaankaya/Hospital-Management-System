package db;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real Swing interaction smoke against the existing isolated MySQL fixture.
 * Assertions exercise actual buttons and EDT-safe asynchronous DAO callbacks.
 */
public final class GuiWorkflowTest {
    private static MainFrame frame;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void onUi(Runnable operation) throws Exception {
        SwingUtilities.invokeAndWait(operation);
    }

    private static boolean waitUntil(java.util.function.BooleanSupplier condition)
            throws Exception {
        for (int retry = 0; retry < 100; retry++) {
            AtomicBoolean result = new AtomicBoolean();
            onUi(() -> result.set(condition.getAsBoolean()));
            if (result.get()) return true;
            Thread.sleep(160);
        }
        return false;
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component item : root.getComponents()) {
            if (type.isInstance(item)) return type.cast(item);
            if (item instanceof Container) {
                T nested = find((Container) item, type);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static JTextField field(String name) {
        return searchField(frame, name);
    }

    private static JTextField searchField(Container root, String name) {
        for (Component item : root.getComponents()) {
            if (item instanceof JTextField
                    && name.equals(item.getAccessibleContext().getAccessibleName())) {
                return (JTextField) item;
            }
            if (item instanceof Container) {
                JTextField nested = searchField((Container) item, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static JButton button(Container root, String caption) {
        for (Component item : root.getComponents()) {
            if (item instanceof JButton && caption.equals(((JButton) item).getText())) {
                return (JButton) item;
            }
            if (item instanceof Container) {
                JButton nested = button((Container) item, caption);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void snapshot(String name) {
        try {
            File output = new File("build/screenshots/" + name + ".png");
            output.getParentFile().mkdirs();
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                frame.paintAll(graphics);
            } finally {
                graphics.dispose();
            }
            check(ImageIO.write(image, "png", output), "PNG renderer unavailable");
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot render Swing screenshot", ex);
        }
    }

    public static void main(String[] args) throws Exception {
        onUi(() -> {
            DesktopStylesTestSetup();
            frame = new MainFrame();
            frame.setVisible(true);
            snapshot("login");
            LoginPanel login = find(frame, LoginPanel.class);
            check(login != null && login.getRoleTabCount() == 3,
                    "Login screen is missing its roles");
            check(field("Legacy numeric ID") != null, "Numeric ID field missing");
            check(field("First name") != null, "First name field missing");
            check(field("Last name") != null, "Last name field missing");
            field("Legacy numeric ID").setText("1002002");
            field("First name").setText("Anna");
            field("Last name").setText("Testpatient");
            JButton loginButton = button(login, "Open patient workspace");
            check(loginButton != null, "Patient login button missing");
            loginButton.doClick();
        });

        try {
            check(waitUntil(() -> Integer.valueOf(1002002).equals(
                    frame.getCurrentPatientSsn())), "Patient GUI login did not complete");
            onUi(() -> {
                JButton book = button(find(frame, PatientDashboardPanel.class),
                        "Book an appointment");
                check(book != null, "Booking navigation button missing");
                book.doClick();
            });
            check(waitUntil(() -> {
                AppointmentPanel p = find(frame, AppointmentPanel.class);
                JButton book = button(p, "Confirm appointment");
                return book != null && book.isEnabled();
            }), "Available booking slots did not reach the GUI");

            onUi(() -> {
                snapshot("patient-booking");
                JButton back = button(find(frame, AppointmentPanel.class),
                        "Back to patient");
                check(back != null, "Patient booking back button missing");
                back.doClick();
                JButton update = button(find(frame, PatientDashboardPanel.class),
                        "Update contact address");
                check(update != null, "Patient address button missing");
                update.doClick();
                snapshot("patient-address");
                JButton signOut = button(find(frame, PatientDashboardPanel.class),
                        "Sign out");
                // Explicit navigation avoids clicking a button on a hidden screen.
                frame.showPanel(MainFrame.LOGIN_PANEL);
                check(frame.getCurrentPatientSsn() == null, "Sign out retained patient session");

                JTabbedPane tabs = find(find(frame, LoginPanel.class), JTabbedPane.class);
                check(tabs != null, "Role tabs missing");
                tabs.setSelectedIndex(1);
                JTextField numeric = field("Legacy numeric ID");
                JTextField first = field("First name");
                JTextField last = field("Last name");
                check(numeric != null && first != null && last != null,
                        "Doctor identity controls missing");
                numeric.setText("1001001");
                first.setText("Ada");
                last.setText("Lovelace");
                JButton doctorLogin = button(find(frame, LoginPanel.class),
                        "Open doctor workspace");
                check(doctorLogin != null, "Doctor login button missing");
                doctorLogin.doClick();
            });
            check(waitUntil(() -> Integer.valueOf(1001001).equals(
                    frame.getCurrentDoctorSsn())), "Doctor GUI login did not complete");
            onUi(() -> {
                snapshot("doctor-dashboard");
                JButton openCancellation = button(find(frame, DoctorDashboardPanel.class),
                        "View or cancel appointments");
                check(openCancellation != null, "Doctor appointment navigation missing");
                openCancellation.doClick();
            });
            check(waitUntil(() -> find(frame, CancelAppointmentPanel.class).isShowing()),
                    "Doctor cancellation page not visible");
            onUi(() -> {
                snapshot("doctor-appointments");
                frame.showPanel(MainFrame.LOGIN_PANEL);
                frame.setAdminAuthenticated();
                frame.showPanel(MainFrame.INSERT_MENU_PANEL);
                snapshot("admin-dashboard");
            });
            System.out.println("Hospital patient, doctor and admin GUI interaction tests passed");
        } finally {
            onUi(() -> {
                if (frame != null) frame.dispose();
            });
        }
    }

    private static void DesktopStylesTestSetup() {
        ModernSwingUtils.setModernLookAndFeel();
    }
}
