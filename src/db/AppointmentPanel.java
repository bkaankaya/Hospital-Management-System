package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AppointmentPanel extends JPanel {
    private MainFrame mainFrame;

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

        JComboBox<String> deptCombo = new JComboBox<>();
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

        JComboBox<String> doctorCombo = new JComboBox<>();
        doctorCombo.setBounds(21, 176, 204, 46);
        DoctorDAO docDao = new DoctorDAO();
        if (deptCombo.getItemCount() > 0) {
            java.util.List<String> docs = docDao.getDoctorList(deptCombo.getItemAt(0));
            if (docs != null) {
                for (String doc : docs) {
                    doctorCombo.addItem(doc);
                }
            }
        }
        add(doctorCombo);

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

        JTextField dateField = new JTextField();
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
            }
        });
        add(datePickerBtn);

        JLabel timeLabel = new JLabel("Choose Time");
        timeLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        timeLabel.setBounds(21, 331, 204, 29);
        add(timeLabel);

        JComboBox<String> timeCombo = new JComboBox<>();
        timeCombo.setBounds(21, 371, 204, 46);
        for (int i = 9; i <= 17; i++) {
            timeCombo.addItem(String.format("%02d:00", i));
            if (i != 17) timeCombo.addItem(String.format("%02d:30", i));
        }
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
                // Randevu oluşturma işlemi; DAO çağrısı burada yapılmalı.
                JOptionPane.showMessageDialog(null, "SUCCESSFUL!", "SUCCESSFUL", JOptionPane.INFORMATION_MESSAGE);
                mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
            }
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL));
    }
}
