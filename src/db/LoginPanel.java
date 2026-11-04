package db;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/** Password-only authentication for patients, doctors and local administrators. */
public class LoginPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTabbedPane tabs = new JTabbedPane();
    private int adminFailures;
    private long adminBlockedUntil;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabs.setBackground(Color.WHITE);
        tabs.addTab("Patient", identityTab(SecureAccounts.Kind.PATIENT));
        tabs.addTab("Doctor", identityTab(SecureAccounts.Kind.DOCTOR));
        tabs.addTab("Administrator", adminTab());
        add(DesktopStyles.page("Hospital Management System",
                "Sign in with your 11-digit identifier and password", tabs), BorderLayout.CENTER);
    }

    private JPanel identityTab(SecureAccounts.Kind role) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        JTextField account = DesktopStyles.textField("Account ID");
        JPasswordField password = new JPasswordField(20);
        DesktopStyles.styleInput(password, "Account password");
        DesktopStyles.row(form, 0, "11-digit ID or legacy account ID", account);
        DesktopStyles.row(form, 1, "Password", password);

        JPanel section = new JPanel(new BorderLayout(0, 14));
        section.setOpaque(true);
        section.setBackground(Color.WHITE);
        JLabel help = new JLabel("Passwords must be issued by the administrator.");
        help.setForeground(DesktopStyles.MUTED);
        section.add(help, BorderLayout.NORTH);
        section.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 9));
        footer.setOpaque(false);
        JButton submit = DesktopStyles.button(role == SecureAccounts.Kind.PATIENT
                ? "Open patient workspace" : "Open doctor workspace", true);
        JLabel status = new JLabel(" ");
        status.setForeground(DesktopStyles.MUTED);
        footer.add(submit, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        section.add(footer, BorderLayout.SOUTH);

        submit.addActionListener(event -> {
            final String id = account.getText().trim();
            final char[] entered = password.getPassword();
            password.setText("");
            if (id.isEmpty() || entered.length == 0) {
                Arrays.fill(entered, '\0');
                status.setText("Enter your account ID and password.");
                return;
            }
            submit.setEnabled(false);
            status.setText("Checking credentials...");
            new SwingWorker<Integer, Void>() {
                @Override protected Integer doInBackground() {
                    try { return SecureAccounts.authenticate(role, id, entered); }
                    finally { Arrays.fill(entered, '\0'); }
                }
                @Override protected void done() {
                    submit.setEnabled(true);
                    try {
                        int internalId = get();
                        if (internalId <= 0) {
                            status.setText("Sign-in failed. Check credentials or contact the administrator.");
                            return;
                        }
                        if (role == SecureAccounts.Kind.PATIENT) {
                            mainFrame.setCurrentPatientSsn(internalId);
                            mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
                        } else {
                            mainFrame.setCurrentDoctorSsn(internalId);
                            mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL);
                        }
                        account.setText("");
                        status.setText(" ");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        status.setText("Sign-in interrupted.");
                    } catch (ExecutionException ex) {
                        status.setText("Sign-in unavailable. Contact the administrator.");
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
        JLabel status = new JLabel("The administrator password is set locally, not in Git.");
        status.setForeground(DesktopStyles.MUTED);
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.add(submit, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        JPanel section = new JPanel(new BorderLayout(0, 18));
        section.setOpaque(true);
        section.setBackground(Color.WHITE);
        section.add(form, BorderLayout.CENTER);
        section.add(footer, BorderLayout.SOUTH);
        submit.addActionListener(event -> {
            char[] entered = password.getPassword();
            password.setText("");
            try {
                if (System.currentTimeMillis() < adminBlockedUntil) {
                    status.setText("Too many attempts. Try again later.");
                    return;
                }
                String configured = System.getenv("HOSPITAL_ADMIN_PASSWORD");
                if (configured == null || configured.length() < 12) {
                    status.setText("Administrator login is not configured.");
                    return;
                }
                byte[] a = new String(entered).getBytes(StandardCharsets.UTF_8);
                byte[] b = configured.getBytes(StandardCharsets.UTF_8);
                boolean correct = MessageDigest.isEqual(a, b);
                Arrays.fill(a, (byte) 0);
                Arrays.fill(b, (byte) 0);
                if (!correct) {
                    if (++adminFailures >= 5) {
                        adminBlockedUntil = System.currentTimeMillis() + 15 * 60_000L;
                        adminFailures = 0;
                    }
                    status.setText("Sign-in failed.");
                    return;
                }
                adminFailures = 0;
                mainFrame.setAdminAuthenticated();
                mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL);
                status.setText(" ");
            } finally { Arrays.fill(entered, '\0'); }
        });
        return section;
    }

    public int getRoleTabCount() { return tabs.getTabCount(); }
}
