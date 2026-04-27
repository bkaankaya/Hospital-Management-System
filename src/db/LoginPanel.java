package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginPanel extends JPanel {
    private MainFrame mainFrame;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Patient Login ---
        JPanel patientPanel = new JPanel();
        patientPanel.setLayout(null);
        JLabel patientSSNLabel = new JLabel("SSN:");
        patientSSNLabel.setBounds(30, 30, 80, 25);
        patientPanel.add(patientSSNLabel);
        JTextField patientSSNField = new JTextField();
        patientSSNField.setBounds(120, 30, 150, 25);
        patientPanel.add(patientSSNField);
        JLabel patientNameLabel = new JLabel("Name:");
        patientNameLabel.setBounds(30, 70, 80, 25);
        patientPanel.add(patientNameLabel);
        JTextField patientNameField = new JTextField();
        patientNameField.setBounds(120, 70, 150, 25);
        patientPanel.add(patientNameField);
        JLabel patientSurnameLabel = new JLabel("Surname:");
        patientSurnameLabel.setBounds(30, 110, 80, 25);
        patientPanel.add(patientSurnameLabel);
        JTextField patientSurnameField = new JTextField();
        patientSurnameField.setBounds(120, 110, 150, 25);
        patientPanel.add(patientSurnameField);
        JButton patientLoginButton = new JButton("Login");
        patientLoginButton.setBounds(120, 150, 100, 30);
        patientPanel.add(patientLoginButton);
        patientLoginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (patientSSNField.getText().isEmpty() || patientNameField.getText().isEmpty()
                        || patientSurnameField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please fill in all fields!", "Warning",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    try {
                        int ssn = Integer.parseInt(patientSSNField.getText().trim());
                        String name = patientNameField.getText().trim();
                        String surname = patientSurnameField.getText().trim();
                        PatientDAO dao = new PatientDAO();
                        if (dao.checkPatientLogin(ssn, name, surname)) {
                            mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
                        } else {
                            JOptionPane.showMessageDialog(null, "Invalid Patient SSN, Name, or Surname!", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "SSN must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // --- Doctor Login ---
        JPanel doctorPanel = new JPanel();
        doctorPanel.setLayout(null);
        JLabel doctorSSNLabel = new JLabel("SSN:");
        doctorSSNLabel.setBounds(30, 30, 80, 25);
        doctorPanel.add(doctorSSNLabel);
        JTextField doctorSSNField = new JTextField();
        doctorSSNField.setBounds(120, 30, 150, 25);
        doctorPanel.add(doctorSSNField);
        JLabel doctorNameLabel = new JLabel("Name:");
        doctorNameLabel.setBounds(30, 70, 80, 25);
        doctorPanel.add(doctorNameLabel);
        JTextField doctorNameField = new JTextField();
        doctorNameField.setBounds(120, 70, 150, 25);
        doctorPanel.add(doctorNameField);
        JLabel doctorSurnameLabel = new JLabel("Surname:");
        doctorSurnameLabel.setBounds(30, 110, 80, 25);
        doctorPanel.add(doctorSurnameLabel);
        JTextField doctorSurnameField = new JTextField();
        doctorSurnameField.setBounds(120, 110, 150, 25);
        doctorPanel.add(doctorSurnameField);
        JButton doctorLoginButton = new JButton("Login");
        doctorLoginButton.setBounds(120, 150, 100, 30);
        doctorPanel.add(doctorLoginButton);
        doctorLoginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (doctorSSNField.getText().isEmpty() || doctorNameField.getText().isEmpty()
                        || doctorSurnameField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please fill in all fields!", "Warning",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    try {
                        int ssn = Integer.parseInt(doctorSSNField.getText().trim());
                        String name = doctorNameField.getText().trim();
                        String surname = doctorSurnameField.getText().trim();
                        DoctorDAO dao = new DoctorDAO();
                        if (dao.checkDoctorLogin(ssn, name, surname)) {
                            // mainFrame.loginSuccessful() routes to INSERT_MENU_PANEL, but original code routed to DOCTOR_DASHBOARD_PANEL
                            mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL);
                        } else {
                            JOptionPane.showMessageDialog(null, "Invalid Doctor SSN, Name, or Surname!", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "SSN must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        tabbedPane.addTab("Patient Login", patientPanel);
        tabbedPane.addTab("Doctor Login", doctorPanel);
        add(tabbedPane, BorderLayout.CENTER);
    }
}
