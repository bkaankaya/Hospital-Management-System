package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PatientDashboardPanel extends JPanel {
    private MainFrame mainFrame;

    public PatientDashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JButton getAppointmentButton = new JButton("Get Appointment");
        getAppointmentButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        getAppointmentButton.setBounds(195, 62, 245, 85);
        add(getAppointmentButton);
        getAppointmentButton.addActionListener(e -> mainFrame.showPanel(MainFrame.APPOINTMENT_PANEL));

        JButton updateAddressButton = new JButton("Update Address");
        updateAddressButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        updateAddressButton.setBounds(195, 269, 245, 85);
        add(updateAddressButton);
        updateAddressButton.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_UPDATE_PANEL));

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.LOGIN_PANEL));
    }
}
