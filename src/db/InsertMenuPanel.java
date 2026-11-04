package db;

import javax.swing.*;
import java.awt.*;

public class InsertMenuPanel extends JPanel {
    public InsertMenuPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        JPanel actions = DesktopStyles.actionList();
        JButton patient = DesktopStyles.button("Register a patient", true);
        patient.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_PATIENT_PANEL));
        JButton doctor = DesktopStyles.button("Register a doctor", false);
        doctor.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_DOCTOR_PANEL));
        JButton search = DesktopStyles.button("Search patients", false);
        search.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_NAME_LIKE_PANEL));
        JButton statistics = DesktopStyles.button("Medicine statistics", false);
        statistics.addActionListener(e -> mainFrame.showPanel(MainFrame.MEDICINE_STATS_PANEL));
        JButton logout = DesktopStyles.button("Sign out", false);
        logout.addActionListener(e -> mainFrame.showPanel(MainFrame.LOGIN_PANEL));
        actions.add(patient);
        actions.add(doctor);
        actions.add(search);
        actions.add(statistics);
        actions.add(logout);
        add(DesktopStyles.page("Administration",
                "Manage local demo records • Only accessible after administrator login",
                actions), BorderLayout.CENTER);
    }
}
