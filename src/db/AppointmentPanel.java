package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AppointmentPanel extends JPanel {
    private MainFrame mainFrame;
    private JComboBox<String> deptCombo;
    private JComboBox<String> doctorCombo;
    private JComboBox<String> timeCombo;
    private JTextField dateField;
    private final DoctorDAO docDao = new DoctorDAO();
    private final AppointmentDAO appointmentDao = new AppointmentDAO();

    public AppointmentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JLabel title = new JLabel("GET APPOINTMENT");
        title.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        title.setBounds(21, 11, 204, 36);
        add(title);

        JLabel deptLabel = new JLabel("Choose Department");
        deptLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        deptLabel.setBounds(21, 62, 204, 29);
        add(deptLabel);

        deptCombo = new JComboBox<>();
        deptCombo.setBounds(21, 90, 204, 46);
        DepartmentDAO deptDao = new DepartmentDAO();
        java.util.List<String> depts = deptDao.getDepartmentList();
        if (depts != null) {
            for (String d : depts) {
                deptCombo.addItem(d);
            }
        }
        add(deptCombo);

        JLabel doctorLabel = new JLabel("Choose Doctor");
        doctorLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        doctorLabel.setBounds(21, 147, 204, 29);
        add(doctorLabel);

        doctorCombo = new JComboBox<>();
        doctorCombo.setBounds(21, 176, 204, 46);
        // Shared DAO field handles doctor lookup.
        if (deptCombo.getItemCount() > 0) {
            java.util.List<String> docs = docDao.getDoctorList(deptCombo.getItemAt(0));
            if (docs != null) {
                for (String doc : docs) {
                    doctorCombo.addItem(doc);
                }
            }
        }
        add(doctorCombo);
        doctorCombo.addActionListener(e -> refreshSlots());

        deptCombo.addActionListener(e -> {
            doctorCombo.removeAllItems();
            String selectedDept = (String) deptCombo.getSelectedItem();
            if (selectedDept != null) {
                java.util.List<String> docs = docDao.getDoctorList(selectedDept);
                if (docs != null) {
                    for (String doc : docs) {
                        doctorCombo.addItem(doc);
                    }
                }
            }
        });

        JLabel dateLabel = new JLabel("Choose Date");
        dateLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        dateLabel.setBounds(21, 233, 204, 29);
        add(dateLabel);

        dateField = new JTextField();
        dateField.setBounds(21, 273, 130, 46);
        dateField.setEditable(false);
        add(dateField);

        JButton datePickerBtn = new JButton("Pick");
        datePickerBtn.setBounds(160, 273, 65, 46);
        datePickerBtn.addActionListener(e -> {
            JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
            JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
            dateSpinner.setEditor(dateEditor);
            int option = JOptionPane.showOptionDialog(null, dateSpinner, "Select Date", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (option == JOptionPane.OK_OPTION) {
                java.util.Date selectedDate = (java.util.Date) dateSpinner.getValue();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                dateField.setText(sdf.format(selectedDate));
                refreshSlots();
            }
        });
        add(datePickerBtn);

        JLabel timeLabel = new JLabel("Choose Time");
        timeLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        timeLabel.setBounds(21, 331, 204, 29);
        add(timeLabel);

        timeCombo = new JComboBox<>();
        timeCombo.setBounds(21, 371, 204, 46);
        // Times are loaded from the database after date and doctor selection.
        add(timeCombo);

        JButton confirmButton = new JButton("Confirm");
        confirmButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        confirmButton.setBounds(359, 198, 218, 64);
        add(confirmButton);
        confirmButton.addActionListener(e -> {
            if (deptCombo.getSelectedItem() == null || deptCombo.getSelectedItem().toString().isEmpty() ||
                    doctorCombo.getSelectedItem() == null || doctorCombo.getSelectedItem().toString().isEmpty() ||
                    dateField.getText().isEmpty() ||
                    timeCombo.getSelectedItem() == null || timeCombo.getSelectedItem().toString().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Fill in all the blanks to continue!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                Integer patientId = mainFrame.getCurrentPatientSsn();
                int doctorId = docDao.getDoctorSsn((String) doctorCombo.getSelectedItem());
                if (patientId == null || doctorId <= 0) {
                    JOptionPane.showMessageDialog(this, "Login as a patient and select a valid doctor.",
                            "Booking denied", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Appointment appointment = new Appointment(dateField.getText(),
                        (String) timeCombo.getSelectedItem(), patientId, doctorId);
                if (appointmentDao.bookAppointment(appointment)) {
                    JOptionPane.showMessageDialog(this, "Appointment booked.", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
                } else {
                    JOptionPane.showMessageDialog(this, "Appointment was not saved. The slot may be unavailable.",
                            "Booking failed", JOptionPane.ERROR_MESSAGE);
                    refreshSlots();
                }
            }
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL));
    }
    public void refresh() {
        dateField.setText("");
        timeCombo.removeAllItems();
        deptCombo.removeAllItems();
        java.util.List<String> departments = new DepartmentDAO().getDepartmentList();
        if (departments != null) {
            for (String department : departments) deptCombo.addItem(department);
        }
    }

    private void refreshSlots() {
        if (timeCombo == null) return;
        timeCombo.removeAllItems();
        if (dateField == null || dateField.getText().trim().isEmpty()
                || doctorCombo.getSelectedItem() == null) return;
        int doctorId = docDao.getDoctorSsn((String) doctorCombo.getSelectedItem());
        if (doctorId <= 0) return;
        java.util.List<String> times = appointmentDao.getTimeList(doctorId, dateField.getText());
        if (times != null) {
            for (String time : times) timeCombo.addItem(time);
        }
    }

}
