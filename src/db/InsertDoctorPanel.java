package db;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Java Swing-only doctor registration form with async department loading. */
public class InsertDoctorPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField id = DesktopStyles.textField("Legacy doctor ID");
    private final JTextField first = DesktopStyles.textField("Doctor first name");
    private final JTextField last = DesktopStyles.textField("Doctor last name");
    private final JComboBox<String> gender = new JComboBox<>(new String[] {"Female", "Male", "Other"});
    private final JTextField address = DesktopStyles.textField("Doctor address");
    private final JTextField birth = DesktopStyles.textField("Doctor date of birth YYYY-MM-DD");
    private final JComboBox<String> departments = new JComboBox<>();
    private final JButton save = DesktopStyles.button("Register doctor", true);
    private final JLabel status = new JLabel("Select an existing department.");
    private int refreshGeneration;

    public InsertDoctorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        DesktopStyles.row(form, 0, "Numeric ID", id);
        DesktopStyles.row(form, 1, "First name", first);
        DesktopStyles.row(form, 2, "Last name", last);
        DesktopStyles.row(form, 3, "Gender", gender);
        DesktopStyles.row(form, 4, "Address", address);
        DesktopStyles.row(form, 5, "Date of birth", birth);
        DesktopStyles.row(form, 6, "Department", departments);

        JButton back = DesktopStyles.button("Back to administration", false);
        back.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
        save.addActionListener(e -> register());
        save.setEnabled(false);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(save);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        status.setForeground(DesktopStyles.MUTED);
        footer.add(status, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.add(form, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        add(DesktopStyles.page("Register a doctor",
                "Keep existing departments and numeric ID schema", content),
                BorderLayout.CENTER);
    }

    public void refreshDepartments() {
        final int generation = ++refreshGeneration;
        departments.removeAllItems();
        save.setEnabled(false);
        status.setText("Loading departments...");
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() {
                return new DepartmentDAO().getDepartmentList();
            }
            @Override protected void done() {
                if (generation != refreshGeneration) return;
                try {
                    List<String> values = get();
                    if (values == null || values.isEmpty()) {
                        status.setText("No departments found; check the local MySQL database.");
                        return;
                    }
                    for (String value : values) departments.addItem(value);
                    save.setEnabled(true);
                    status.setText("Select a department and enter a valid date of birth.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Department lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Unable to load departments.");
                }
            }
        }.execute();
    }

    private void register() {
        final int numericId;
        final LocalDate birthDate;
        try {
            numericId = Integer.parseInt(id.getText().trim());
            if (numericId <= 0) throw new IllegalArgumentException("ID must be positive.");
        } catch (NumberFormatException ex) {
            status.setText("The legacy ID must be a positive 32-bit integer.");
            return;
        } catch (IllegalArgumentException ex) {
            status.setText(ex.getMessage());
            return;
        }
        try {
            birthDate = LocalDate.parse(birth.getText().trim());
            if (birthDate.isAfter(LocalDate.now())) {
                status.setText("Birth date cannot be in the future.");
                return;
            }
        } catch (DateTimeParseException ex) {
            status.setText("Date of birth must follow YYYY-MM-DD.");
            return;
        }
        String givenName = first.getText().trim();
        String familyName = last.getText().trim();
        String location = address.getText().trim();
        String dept = (String) departments.getSelectedItem();
        if (givenName.isEmpty() || familyName.isEmpty() || location.isEmpty()
                || location.length() > 500 || dept == null) {
            status.setText("Provide a valid name, address (up to 500 characters) and department.");
            return;
        }
        Doctor doctor = new Doctor(numericId, givenName, familyName,
                (String) gender.getSelectedItem(), location, birthDate.toString(), dept);
        save.setEnabled(false);
        status.setText("Saving doctor...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return new DoctorDAO().tryInsertDoctor(doctor);
            }
            @Override protected void done() {
                save.setEnabled(true);
                try {
                    if (get()) {
                        id.setText("");
                        first.setText("");
                        last.setText("");
                        address.setText("");
                        birth.setText("");
                        status.setText("Doctor registered.");
                        mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL);
                    } else {
                        status.setText("Doctor could not be saved (duplicate ID or database error).");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Save interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Save failed.");
                }
            }
        }.execute();
    }
}
