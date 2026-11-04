package db;

import javax.swing.*;
import java.awt.*;

public class DoctorDashboardPanel extends JPanel {
    public DoctorDashboardPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        JPanel actions = DesktopStyles.actionList();
        JButton cancel = DesktopStyles.button("View or cancel appointments", true);
        cancel.addActionListener(e -> mainFrame.showPanel(MainFrame.CANCEL_APPOINTMENT_PANEL));
        JButton address = DesktopStyles.button("Update contact address", false);
        address.addActionListener(e -> mainFrame.showPanel(MainFrame.DOCTOR_UPDATE_PANEL));
        JButton logout = DesktopStyles.button("Sign out", false);
        logout.addActionListener(e -> mainFrame.showPanel(MainFrame.LOGIN_PANEL));
        actions.add(cancel);
        actions.add(address);
        actions.add(logout);
        add(DesktopStyles.page("Doctor workspace",
                "Manage the appointment schedule and personal information", actions), BorderLayout.CENTER);
    }
}
