package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class InsertMenuPanel extends JPanel {
    private MainFrame mainFrame;

    public InsertMenuPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JLabel label = new JLabel("INSERT MENU");
        label.setFont(new Font("Trebuchet MS", Font.BOLD, 20));
        label.setBounds(30, 30, 200, 30);
        add(label);

        JButton insertPatientButton = new JButton("Insert Patient");
        insertPatientButton.setBounds(200, 150, 200, 50);
        add(insertPatientButton);
        insertPatientButton.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_PATIENT_PANEL));

        JButton insertDoctorButton = new JButton("Insert Doctor");
        insertDoctorButton.setBounds(200, 250, 200, 50);
        add(insertDoctorButton);
        insertDoctorButton.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_DOCTOR_PANEL));
    }
}
