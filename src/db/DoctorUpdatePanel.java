package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DoctorUpdatePanel extends JPanel {
    private MainFrame mainFrame;
    private JTextField addressInput;

    public DoctorUpdatePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JLabel title = new JLabel("UPDATE ADDRESS");
        title.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        title.setBounds(10, 11, 200, 44);
        add(title);

        JLabel newAddressLabel = new JLabel("NEW ADDRESS");
        newAddressLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        newAddressLabel.setBounds(40, 140, 185, 44);
        add(newAddressLabel);

        addressInput = new JTextField();
        addressInput.setBounds(246, 146, 241, 38);
        add(addressInput);

        JButton updateButton = new JButton("UPDATE");
        updateButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        updateButton.setBounds(234, 357, 192, 44);
        add(updateButton);
        updateButton.addActionListener(e -> {
            if (addressInput.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Fill in all the blanks to continue!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                // Doktor adres güncelleme işlemi; DAO çağrısı burada yapılmalı.
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
