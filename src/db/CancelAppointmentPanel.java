package db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Doctor-specific, sortable schedule and protected cancellation screen.
 * No PII is written to logs, and UI selection never controls an arbitrary SQL clause.
 */
public class CancelAppointmentPanel extends JPanel {
    private final MainFrame mainFrame;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Appointment #", "Date", "Time", "Patient"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel("Sign in as a doctor to manage appointments.");
    private final JButton cancel = DesktopStyles.button("Cancel selected appointment", true);
    private final JButton reload = DesktopStyles.button("Refresh", false);
    private int generation;

    public CancelAppointmentPanel(MainFrame frame) {
        this.mainFrame = frame;
        setLayout(new BorderLayout());
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cancel.setEnabled(table.getSelectedRow() >= 0);
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(740, 260));
        cancel.setEnabled(false);
        cancel.addActionListener(e -> cancelSelected());

        JButton back = DesktopStyles.button("Back to doctor", false);
        back.addActionListener(e -> frame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL));
        reload.addActionListener(e -> refresh());
        JPanel buttons = new JPanel(new GridLayout(1, 3, 10, 0));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(reload);
        buttons.add(cancel);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        status.setForeground(DesktopStyles.MUTED);
        footer.add(status, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(scroll, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        add(DesktopStyles.page("Doctor schedule",
                "Select an upcoming appointment to cancel it", body), BorderLayout.CENTER);
    }

    public void refresh() {
        final Integer doctorId = mainFrame.getCurrentDoctorSsn();
        final int token = ++generation;
        model.setRowCount(0);
        cancel.setEnabled(false);
        reload.setEnabled(false);
        if (doctorId == null) {
            status.setText("Doctor sign-in is required.");
            reload.setEnabled(true);
            return;
        }
        status.setText("Loading your appointments...");
        new SwingWorker<List<String[]>, Void>() {
            @Override protected List<String[]> doInBackground() {
                return GatewayBridge.doctorSchedule(doctorId);
            }
            @Override protected void done() {
                if (token != generation || !doctorId.equals(mainFrame.getCurrentDoctorSsn())) return;
                reload.setEnabled(true);
                try {
                    List<String[]> rows = get();
                    if (rows == null) {
                        status.setText("Unable to load appointments from MySQL.");
                        return;
                    }
                    for (String[] row : rows) model.addRow(row);
                    status.setText(rows.isEmpty()
                            ? "No upcoming appointments." : rows.size() + " appointment(s), maximum 100 displayed.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Request interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Schedule could not be loaded.");
                }
            }
        }.execute();
    }

    private void cancelSelected() {
        int index = table.getSelectedRow();
        final Integer doctorId = mainFrame.getCurrentDoctorSsn();
        if (doctorId == null || index < 0) {
            status.setText("Select one of your upcoming appointments.");
            return;
        }
        final int appointmentId;
        try {
            appointmentId = Integer.parseInt(String.valueOf(
                    model.getValueAt(table.convertRowIndexToModel(index), 0)));
        } catch (NumberFormatException ex) {
            status.setText("Invalid appointment selection.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel appointment #" + appointmentId + "?", "Confirm cancellation",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        cancel.setEnabled(false);
        reload.setEnabled(false);
        status.setText("Cancelling selected appointment...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return GatewayBridge.cancel(appointmentId, doctorId);
            }
            @Override protected void done() {
                if (!doctorId.equals(mainFrame.getCurrentDoctorSsn())) return;
                try {
                    status.setText(get() ? "Appointment cancelled." :
                            "Cannot cancel: already completed, linked to a record, or unavailable.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Cancellation interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Cancellation failed.");
                }
                refresh();
            }
        }.execute();
    }
}
