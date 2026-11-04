package db;

import javax.swing.*;
import java.awt.*;

public class PatientDashboardPanel extends JPanel {
    public PatientDashboardPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        JPanel actions = DesktopStyles.actionList();
        JButton appointment = DesktopStyles.button("Book an appointment", true);
        appointment.addActionListener(e -> mainFrame.showPanel(MainFrame.APPOINTMENT_PANEL));
        JButton address = DesktopStyles.button("Update contact address", false);
        address.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_UPDATE_PANEL));
        JButton logout = DesktopStyles.button("Sign out", false);
        logout.addActionListener(e -> mainFrame.showPanel(MainFrame.LOGIN_PANEL));
        actions.add(appointment);
        actions.add(address);
        actions.add(logout);
        add(DesktopStyles.page("Patient workspace",
                "Manage your appointments and contact information", actions), BorderLayout.CENTER);
    }
}
