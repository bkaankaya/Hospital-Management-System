package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CancelAppointmentPanel extends JPanel {
    private MainFrame mainFrame;
    private JComboBox<String> appIdCombo;

    public CancelAppointmentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JLabel title = new JLabel("Cancel Appointment");
        title.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        title.setBounds(21, 11, 193, 35);
        add(title);

        JLabel appointmentIdLabel = new JLabel("Appointment ID");
        appointmentIdLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        appointmentIdLabel.setBounds(28, 114, 173, 49);
        add(appointmentIdLabel);

        appIdCombo = new JComboBox<>();
        appIdCombo.setBounds(212, 124, 241, 35);
        add(appIdCombo);

        JButton cancelButton = new JButton("CANCEL");
        cancelButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        cancelButton.setBounds(221, 325, 222, 74);
        add(cancelButton);
        cancelButton.addActionListener(e -> {
            if (appIdCombo.getSelectedItem() == null || appIdCombo.getSelectedItem().toString().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Fill in all the blanks to continue!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                Integer doctorId = mainFrame.getCurrentDoctorSsn();
                if (doctorId == null) {
                    JOptionPane.showMessageDialog(this, "Please log in as a doctor.",
                            "Cancellation denied", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int appointmentId = Integer.parseInt((String) appIdCombo.getSelectedItem());
                if (new AppointmentDAO().cancelDoctorAppointment(appointmentId, doctorId)) {
                    JOptionPane.showMessageDialog(this, "Appointment canceled.", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL);
                } else {
                    JOptionPane.showMessageDialog(this, "Appointment could not be canceled.",
                            "Cancellation failed", JOptionPane.ERROR_MESSAGE);
                    refresh();
                }
            }
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL));
    }
    public void refresh() {
        appIdCombo.removeAllItems();
        Integer doctorId = mainFrame.getCurrentDoctorSsn();
        if (doctorId == null) return;
        java.util.List<String> ids = new AppointmentDAO().getDocAppointments(doctorId);
        if (ids != null) {
            for (String id : ids) appIdCombo.addItem(id);
        }
    }

}
