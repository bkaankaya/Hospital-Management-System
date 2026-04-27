package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DoctorDashboardPanel extends JPanel {
    private MainFrame mainFrame;

    public DoctorDashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JButton cancelAppButton = new JButton("Cancel Appointment");
        cancelAppButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        cancelAppButton.setBounds(201, 72, 245, 85);
        add(cancelAppButton);
        cancelAppButton.addActionListener(e -> mainFrame.showPanel(MainFrame.CANCEL_APPOINTMENT_PANEL));

        JButton updateAddressButton = new JButton("Update Address");
        updateAddressButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        updateAddressButton.setBounds(201, 279, 245, 85);
        add(updateAddressButton);
        updateAddressButton.addActionListener(e -> mainFrame.showPanel(MainFrame.DOCTOR_UPDATE_PANEL));

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.LOGIN_PANEL));
    }
}
