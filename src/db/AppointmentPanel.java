package db;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Responsive appointment booking UI. JDBC work must never block Swing's event thread. */
public class AppointmentPanel extends JPanel {
    private final MainFrame mainFrame;
    private final DoctorDAO doctors = new DoctorDAO();
    private final AppointmentDAO appointments = new AppointmentDAO();
    private final JComboBox<String> department = new JComboBox<>();
    private final JComboBox<String> doctor = new JComboBox<>();
    private final JComboBox<String> date = new JComboBox<>();
    private final JComboBox<String> time = new JComboBox<>();
    private final JLabel status = new JLabel("Select a department, doctor, date and available time.");
    private final JButton confirm = DesktopStyles.button("Confirm appointment", true);
    private int departmentGeneration;
    private int doctorGeneration;
    private int slotsGeneration;
    private int selectedDoctorId;
    private boolean settingControls;

    public AppointmentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        DesktopStyles.row(fields, 0, "Department", department);
        DesktopStyles.row(fields, 1, "Doctor", doctor);
        DesktopStyles.row(fields, 2, "Date", date);
        DesktopStyles.row(fields, 3, "Available time", time);

        for (String value : appointments.getDateList()) date.addItem(value);
        if (date.getItemCount() > 1) date.setSelectedIndex(1);
        department.addActionListener(e -> {
            if (!settingControls) loadDoctors();
        });
        doctor.addActionListener(e -> {
            if (!settingControls) loadTimes();
        });
        date.addActionListener(e -> {
            if (!settingControls) loadTimes();
        });

        JPanel controls = new JPanel(new GridLayout(1, 2, 12, 0));
        controls.setOpaque(false);
        JButton back = DesktopStyles.button("Back to patient", false);
        back.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL));
        controls.add(back);
        controls.add(confirm);
        confirm.setEnabled(false);
        confirm.addActionListener(e -> bookSelectedSlot());

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        status.setForeground(DesktopStyles.MUTED);
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(controls, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        add(DesktopStyles.page("Book an appointment",
                "Available appointments are limited to the next 14 days", content),
                BorderLayout.CENTER);
    }

    /** Called when the patient opens the booking page; reruns reflect new doctors. */
    public void refresh() {
        departmentGeneration++;
        doctorGeneration++;
        slotsGeneration++;
        selectedDoctorId = 0;
        confirm.setEnabled(false);
        settingControls = true;
        department.removeAllItems();
        doctor.removeAllItems();
        time.removeAllItems();
        settingControls = false;
        status.setText("Loading departments...");
        final int generation = departmentGeneration;
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() {
                return GatewayBridge.departments();
            }
            @Override protected void done() {
                if (generation != departmentGeneration) return;
                try {
                    List<String> values = get();
                    if (values == null || values.isEmpty()) {
                        status.setText("No departments found. Check your MySQL connection and schema.");
                        return;
                    }
                    settingControls = true;
                    for (String value : values) department.addItem(value);
                    settingControls = false;
                    loadDoctors();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Department lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Unable to load departments.");
                } finally {
                    settingControls = false;
                }
            }
        }.execute();
    }

    private void loadDoctors() {
        final String selected = (String) department.getSelectedItem();
        final int generation = ++doctorGeneration;
        ++slotsGeneration;
        selectedDoctorId = 0;
        confirm.setEnabled(false);
        settingControls = true;
        doctor.removeAllItems();
        time.removeAllItems();
        settingControls = false;
        if (selected == null) return;
        status.setText("Loading doctors...");
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() {
                return GatewayBridge.doctors(selected);
            }
            @Override protected void done() {
                if (generation != doctorGeneration) return;
                try {
                    List<String> values = get();
                    if (values == null || values.isEmpty()) {
                        status.setText("No doctors found for this department.");
                        return;
                    }
                    settingControls = true;
                    for (String value : values) doctor.addItem(value);
                    settingControls = false;
                    loadTimes();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Doctor lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Unable to load doctors.");
                } finally {
                    settingControls = false;
                }
            }
        }.execute();
    }

    private void loadTimes() {
        final String doctorName = (String) doctor.getSelectedItem();
        final String selectedDate = (String) date.getSelectedItem();
        final int generation = ++slotsGeneration;
        selectedDoctorId = 0;
        confirm.setEnabled(false);
        time.removeAllItems();
        if (doctorName == null || selectedDate == null) return;
        status.setText("Checking available times...");
        new SwingWorker<Slots, Void>() {
            @Override protected Slots doInBackground() {
                int id = GatewayBridge.doctorId(doctorName);
                return id > 0 ? new Slots(id, GatewayBridge.slots(id, selectedDate))
                        : new Slots(0, null);
            }
            @Override protected void done() {
                if (generation != slotsGeneration) return;
                try {
                    Slots result = get();
                    if (result.id <= 0 || result.times == null) {
                        status.setText("Could not load available times from MySQL.");
                        return;
                    }
                    selectedDoctorId = result.id;
                    for (String value : result.times) time.addItem(value);
                    confirm.setEnabled(time.getItemCount() > 0);
                    status.setText(time.getItemCount() == 0
                            ? "No open appointments on this date."
                            : time.getItemCount() + " available time(s).");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Availability lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Unable to load appointment times.");
                }
            }
        }.execute();
    }

    private void bookSelectedSlot() {
        Integer patientId = mainFrame.getCurrentPatientSsn();
        String selectedDate = (String) date.getSelectedItem();
        String selectedTime = (String) time.getSelectedItem();
        int doctorId = selectedDoctorId;
        if (patientId == null || doctorId <= 0 || selectedDate == null || selectedTime == null) {
            status.setText("Please sign in as a patient and choose an available appointment.");
            return;
        }
        confirm.setEnabled(false);
        status.setText("Saving the appointment...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return GatewayBridge.book(new Appointment(
                        selectedDate, selectedTime, patientId, doctorId));
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(AppointmentPanel.this,
                                "Appointment booked successfully.", "Saved",
                                JOptionPane.INFORMATION_MESSAGE);
                        mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
                    } else {
                        status.setText("The slot is unavailable or the booking failed.");
                        loadTimes();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Booking interrupted.");
                    loadTimes();
                } catch (ExecutionException ex) {
                    status.setText("Booking failed. Please refresh availability.");
                    loadTimes();
                }
            }
        }.execute();
    }

    private static final class Slots {
        private final int id;
        private final List<String> times;
        private Slots(int id, List<String> times) {
            this.id = id;
            this.times = times;
        }
    }
}
