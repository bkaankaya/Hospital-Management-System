package db;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Responsive login for the existing legacy identity model.
 * Patient/doctor identity fields are NOT password authentication.
 * Use only with synthetic data until a credential migration is designed.
 */
public class LoginPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTabbedPane tabs = new JTabbedPane();

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabs.addTab("Patient", identityTab(false));
        tabs.addTab("Doctor", identityTab(true));
        tabs.addTab("Administrator", adminTab());
        add(DesktopStyles.page("Hospital Management System",
                "Select your role to continue • Local demonstration", tabs), BorderLayout.CENTER);
    }

    private JPanel identityTab(boolean doctor) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        JTextField id = DesktopStyles.textField("Legacy numeric ID");
        JTextField first = DesktopStyles.textField("First name");
        JTextField last = DesktopStyles.textField("Last name");
        DesktopStyles.row(form, 0, "Numeric ID", id);
        DesktopStyles.row(form, 1, "First name", first);
        DesktopStyles.row(form, 2, "Last name", last);

        JPanel section = new JPanel(new BorderLayout(0, 14));
        section.setOpaque(false);
        JLabel help = new JLabel("Legacy local identity lookup; no password protection.");
        help.setForeground(DesktopStyles.MUTED);
        section.add(help, BorderLayout.NORTH);
        section.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 9));
        footer.setOpaque(false);
        JButton submit = DesktopStyles.button(doctor ? "Open doctor workspace" : "Open patient workspace", true);
        JLabel status = new JLabel(" ");
        status.setForeground(DesktopStyles.MUTED);
        footer.add(submit, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        section.add(footer, BorderLayout.SOUTH);

        submit.addActionListener(event -> {
            final int numericId;
            try {
                numericId = Integer.parseInt(id.getText().trim());
                if (numericId <= 0) throw new NumberFormatException("Non-positive ID");
            } catch (NumberFormatException ex) {
                status.setText("Enter a positive numeric ID (32-bit legacy format).");
                return;
            }
            String firstName = first.getText().trim();
            String lastName = last.getText().trim();
            if (firstName.isEmpty() || lastName.isEmpty()) {
                status.setText("First and last name are required.");
                return;
            }
            submit.setEnabled(false);
            status.setText("Checking the local database...");
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return doctor
                            ? new DoctorDAO().checkDoctorLogin(numericId, firstName, lastName)
                            : new PatientDAO().checkPatientLogin(numericId, firstName, lastName);
                }

                @Override
                protected void done() {
                    submit.setEnabled(true);
                    try {
                        if (!get()) {
                            status.setText("No matching record, or the database is unavailable.");
                            return;
                        }
                        if (doctor) {
                            mainFrame.setCurrentDoctorSsn(numericId);
                            mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL);
                        } else {
                            mainFrame.setCurrentPatientSsn(numericId);
                            mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
                        }
                        status.setText(" ");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        status.setText("Login check interrupted.");
                    } catch (ExecutionException ex) {
                        status.setText("Local database check failed.");
                    }
                }
            }.execute();
        });
        return section;
    }

    private JPanel adminTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        JPasswordField password = new JPasswordField(20);
        DesktopStyles.styleInput(password, "Local administrator password");
        DesktopStyles.row(form, 0, "Admin password", password);
        JButton submit = DesktopStyles.button("Open administration", true);
        JLabel status = new JLabel("Set HOSPITAL_ADMIN_PASSWORD (12+ characters) locally.");
        status.setForeground(DesktopStyles.MUTED);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.add(submit, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        JPanel section = new JPanel(new BorderLayout(0, 18));
        section.setOpaque(false);
        section.add(form, BorderLayout.CENTER);
        section.add(footer, BorderLayout.SOUTH);

        submit.addActionListener(event -> {
            char[] entered = password.getPassword();
            try {
                String configured = System.getenv("HOSPITAL_ADMIN_PASSWORD");
                if (configured == null || configured.length() < 12) {
                    status.setText("Admin login disabled. Configure a strong local password.");
                    return;
                }
                byte[] suppliedBytes = new String(entered).getBytes(StandardCharsets.UTF_8);
                byte[] expectedBytes = configured.getBytes(StandardCharsets.UTF_8);
                boolean valid = MessageDigest.isEqual(expectedBytes, suppliedBytes);
                Arrays.fill(suppliedBytes, (byte) 0);
                Arrays.fill(expectedBytes, (byte) 0);
                if (!valid) {
                    status.setText("Incorrect administrator password.");
                    return;
                }
                password.setText("");
                status.setText(" ");
                mainFrame.setAdminAuthenticated();
                mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL);
            } finally {
                Arrays.fill(entered, '\0');
            }
        });
        return section;
    }

    public int getRoleTabCount() {
        return tabs.getTabCount();
    }
}
