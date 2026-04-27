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
        appIdCombo.addItem("101");
        appIdCombo.addItem("102");
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
                // Randevu iptal işlemi; DAO çağrısı burada yapılmalı.
                JOptionPane.showMessageDialog(null, "SUCCESSFUL!", "SUCCESSFUL", JOptionPane.INFORMATION_MESSAGE);
                mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL);
            }
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL));
    }
}
