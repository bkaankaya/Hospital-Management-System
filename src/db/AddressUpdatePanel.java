package db;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/** Shared Swing form for the existing patient/doctor address update APIs. */
public class AddressUpdatePanel extends JPanel {
    public AddressUpdatePanel(MainFrame mainFrame, boolean doctor) {
        setLayout(new BorderLayout());
        JTextArea address = new JTextArea(5, 26);
        address.setLineWrap(true);
        address.setWrapStyleWord(true);
        address.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        address.getAccessibleContext().setAccessibleName("New contact address");
        JScrollPane scroll = new JScrollPane(address);
        JLabel status = new JLabel("Enter the updated address (up to 500 characters).");
        status.setForeground(DesktopStyles.MUTED);
        JButton submit = DesktopStyles.button("Save address", true);
        JButton back = DesktopStyles.button("Back", false);
        String destination = doctor ? MainFrame.DOCTOR_DASHBOARD_PANEL : MainFrame.PATIENT_DASHBOARD_PANEL;
        back.addActionListener(e -> mainFrame.showPanel(destination));
        submit.addActionListener(e -> {
            Integer id = doctor ? mainFrame.getCurrentDoctorSsn() : mainFrame.getCurrentPatientSsn();
            String value = address.getText().trim();
            if (id == null) {
                status.setText("Please sign in again.");
                return;
            }
            if (value.isEmpty() || value.length() > 500) {
                status.setText("Address must contain between 1 and 500 characters.");
                return;
            }
            submit.setEnabled(false);
            status.setText("Saving address...");
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() {
                    return doctor ? new DoctorDAO().tryUpdateAddress(id, value)
                            : new PatientDAO().tryUpdateAddress(id, value);
                }
                @Override protected void done() {
                    submit.setEnabled(true);
                    try {
                        if (get()) {
                            address.setText("");
                            status.setText("Address updated.");
                            mainFrame.showPanel(destination);
                        } else {
                            status.setText("Unable to update the address in MySQL.");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        status.setText("Save interrupted.");
                    } catch (ExecutionException ex) {
                        status.setText("Address update failed.");
                    }
                }
            }.execute();
        });
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        actions.add(back);
        actions.add(submit);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        footer.add(status, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        add(DesktopStyles.page("Update contact address",
                "Your new address will be saved to the existing MySQL record", content),
                BorderLayout.CENTER);
    }
}
