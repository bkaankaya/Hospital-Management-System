package db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Read-only, patient-scoped Swing schedule view; database queries run off EDT. */
public final class PatientAppointmentsPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JLabel status = new JLabel("Sign in to view your appointments.");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Booking #", "Date", "Time", "Doctor", "Department"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JButton reload = DesktopStyles.button("Refresh", false);
    private int requestId;

    public PatientAppointmentsPanel(MainFrame frame) {
        this.mainFrame = frame;
        setLayout(new BorderLayout());
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, 260));
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        JButton back = DesktopStyles.button("Back to patient", false);
        back.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL));
        reload.addActionListener(e -> refresh());
        actions.add(back);
        actions.add(reload);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        status.setForeground(DesktopStyles.MUTED);
        footer.add(status, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(scroll, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        add(DesktopStyles.page("My upcoming appointments",
                "A read-only view of your next scheduled visits", body), BorderLayout.CENTER);
    }

    public void refresh() {
        final Integer id = mainFrame.getCurrentPatientSsn();
        final int request = ++requestId;
        model.setRowCount(0);
        reload.setEnabled(false);
        if (id == null) {
            status.setText("Patient sign-in required.");
            reload.setEnabled(true);
            return;
        }
        status.setText("Loading your appointments...");
        new SwingWorker<List<String[]>, Void>() {
            @Override protected List<String[]> doInBackground() {
                return GatewayBridge.patientSchedule(id);
            }
            @Override protected void done() {
                if (request != requestId || !id.equals(mainFrame.getCurrentPatientSsn())) return;
                reload.setEnabled(true);
                try {
                    List<String[]> rows = get();
                    if (rows == null) {
                        status.setText("Could not load appointments; check database connectivity.");
                        return;
                    }
                    for (String[] row : rows) model.addRow(row);
                    status.setText(rows.isEmpty() ? "No upcoming appointments."
                            : rows.size() + " upcoming appointment(s), showing at most 100.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Schedule lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Schedule lookup failed.");
                }
            }
        }.execute();
    }
}
