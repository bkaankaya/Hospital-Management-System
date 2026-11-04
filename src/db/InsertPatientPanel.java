package db;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutionException;

/** Responsive patient registration, retaining the existing 32-bit ID schema. */
public class InsertPatientPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField id = DesktopStyles.textField("Legacy patient ID");
    private final JTextField first = DesktopStyles.textField("Patient first name");
    private final JTextField last = DesktopStyles.textField("Patient last name");
    private final JComboBox<String> gender = new JComboBox<>(new String[] {"Female", "Male", "Other"});
    private final JTextField address = DesktopStyles.textField("Patient address");
    private final JTextField birth = DesktopStyles.textField("Patient date of birth YYYY-MM-DD");
    private final JButton save = DesktopStyles.button("Register patient", true);
    private final JLabel status = new JLabel("Enter a valid date as YYYY-MM-DD.");

    public InsertPatientPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        DesktopStyles.row(fields, 0, "Numeric ID", id);
        DesktopStyles.row(fields, 1, "First name", first);
        DesktopStyles.row(fields, 2, "Last name", last);
        DesktopStyles.row(fields, 3, "Gender", gender);
        DesktopStyles.row(fields, 4, "Address", address);
        DesktopStyles.row(fields, 5, "Date of birth", birth);

        JButton back = DesktopStyles.button("Back to administration", false);
        back.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
        save.addActionListener(e -> register());
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
        content.add(fields, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        add(DesktopStyles.page("Register a patient",
                "Use fictional records only; legacy numeric IDs do not accept 11-digit values",
                content), BorderLayout.CENTER);
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
        if (givenName.isEmpty() || familyName.isEmpty()
                || location.isEmpty() || location.length() > 500) {
            status.setText("Provide name, surname and an address of 1–500 characters.");
            return;
        }
        Patient patient = new Patient(numericId, givenName, familyName,
                (String) gender.getSelectedItem(), location, birthDate.toString());
        save.setEnabled(false);
        status.setText("Saving patient...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return new PatientDAO().tryInsertPatient(patient);
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
                        status.setText("Patient registered.");
                        mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL);
                    } else {
                        status.setText("Patient could not be saved (duplicate ID or database error).");
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
