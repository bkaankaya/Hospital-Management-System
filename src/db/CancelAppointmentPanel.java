package db;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Doctor-only appointment cancellation with nonblocking database access. */
public class CancelAppointmentPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JComboBox<String> appointmentId = new JComboBox<>();
    private final JLabel status = new JLabel("Select an appointment to cancel.");
    private final JButton cancel = DesktopStyles.button("Cancel selected appointment", true);
    private int refreshGeneration;

    public CancelAppointmentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        DesktopStyles.row(form, 0, "Appointment ID", appointmentId);
        cancel.setEnabled(false);
        cancel.addActionListener(e -> cancelAppointment());
        JButton back = DesktopStyles.button("Back to doctor", false);
        back.addActionListener(e -> mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL));
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        actions.add(back);
        actions.add(cancel);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        status.setForeground(DesktopStyles.MUTED);
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);
        content.add(form, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        add(DesktopStyles.page("Doctor appointments",
                "Only appointments belonging to the signed-in doctor may be removed", content),
                BorderLayout.CENTER);
    }

    public void refresh() {
        int generation = ++refreshGeneration;
        Integer doctorId = mainFrame.getCurrentDoctorSsn();
        appointmentId.removeAllItems();
        cancel.setEnabled(false);
        if (doctorId == null) {
            status.setText("Doctor sign-in required.");
            return;
        }
        status.setText("Loading appointments...");
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() {
                return new AppointmentDAO().getDocAppointments(doctorId);
            }
            @Override protected void done() {
                if (generation != refreshGeneration
                        || !doctorId.equals(mainFrame.getCurrentDoctorSsn())) return;
                try {
                    List<String> values = get();
                    if (values == null) {
                        status.setText("Could not load appointments from MySQL.");
                        return;
                    }
                    for (String value : values) appointmentId.addItem(value);
                    cancel.setEnabled(appointmentId.getItemCount() > 0);
                    status.setText(appointmentId.getItemCount() == 0
                            ? "No upcoming appointments."
                            : appointmentId.getItemCount() + " appointment(s) found.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Appointment lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Could not read appointments.");
                }
            }
        }.execute();
    }

    private void cancelAppointment() {
        Integer doctorId = mainFrame.getCurrentDoctorSsn();
        String selected = (String) appointmentId.getSelectedItem();
        if (doctorId == null || selected == null) {
            status.setText("Select a valid appointment while signed in as a doctor.");
            return;
        }
        final int id;
        try {
            id = Integer.parseInt(selected);
        } catch (NumberFormatException ex) {
            status.setText("Invalid appointment ID.");
            return;
        }
        int decision = JOptionPane.showConfirmDialog(this,
                "Cancel appointment #" + id + "?", "Confirm cancellation",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (decision != JOptionPane.YES_OPTION) return;
        cancel.setEnabled(false);
        status.setText("Cancelling appointment...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return new AppointmentDAO().cancelDoctorAppointment(id, doctorId);
            }
            @Override protected void done() {
                try {
                    status.setText(get() ? "Appointment cancelled." : "Cancellation not saved.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Cancellation interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Cancellation could not be completed.");
                }
                refresh();
            }
        }.execute();
    }
}
