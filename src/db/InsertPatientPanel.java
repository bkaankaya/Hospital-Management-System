package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class InsertPatientPanel extends JPanel {
    private MainFrame mainFrame;
    private JTextField inputssnp, inputnamep, inputsurnamep, inputgenderp, inputaddressp, inputdobp;

    public InsertPatientPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JLabel title = new JLabel("INSERT PATIENT");
        title.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        title.setBounds(460, 67, 144, 29);
        add(title);

        JLabel textPatSSN = new JLabel("SSN");
        textPatSSN.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 14));
        textPatSSN.setBounds(25, 67, 102, 28);
        add(textPatSSN);

        inputssnp = new JTextField();
        inputssnp.setBounds(178, 68, 174, 29);
        add(inputssnp);

        JLabel textPatName = new JLabel("Name");
        textPatName.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 14));
        textPatName.setBounds(25, 127, 102, 28);
        add(textPatName);

        inputnamep = new JTextField();
        inputnamep.setBounds(178, 128, 174, 29);
        add(inputnamep);

        JLabel textPatSurname = new JLabel("Surname");
        textPatSurname.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 14));
        textPatSurname.setBounds(25, 186, 102, 28);
        add(textPatSurname);

        inputsurnamep = new JTextField();
        inputsurnamep.setBounds(178, 185, 174, 29);
        add(inputsurnamep);

        JLabel textPatGender = new JLabel("Gender");
        textPatGender.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 14));
        textPatGender.setBounds(25, 252, 102, 28);
        add(textPatGender);

        inputgenderp = new JTextField();
        inputgenderp.setBounds(178, 252, 174, 29);
        add(inputgenderp);

        JLabel textPatAddress = new JLabel("Address");
        textPatAddress.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 14));
        textPatAddress.setBounds(25, 318, 102, 28);
        add(textPatAddress);

        inputaddressp = new JTextField();
        inputaddressp.setBounds(178, 318, 174, 29);
        add(inputaddressp);

        JLabel textPatDOB = new JLabel("Date_of_Birth");
        textPatDOB.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 14));
        textPatDOB.setBounds(25, 382, 102, 28);
        add(textPatDOB);

        inputdobp = new JTextField();
        inputdobp.setBounds(178, 382, 174, 29);
        add(inputdobp);

        JButton addButton = new JButton("ADD");
        addButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 20));
        addButton.setBounds(443, 230, 198, 69);
        add(addButton);
        addButton.addActionListener(e -> {
            if (inputssnp.getText().isEmpty() || inputnamep.getText().isEmpty() ||
                    inputsurnamep.getText().isEmpty() || inputgenderp.getText().isEmpty() ||
                    inputaddressp.getText().isEmpty() || inputdobp.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Fill in all the blanks to continue!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                // Hasta ekleme işlemi; DAO çağrısı burada yapılmalı.
                JOptionPane.showMessageDialog(null, "SUCCESSFUL!", "SUCCESSFUL", JOptionPane.INFORMATION_MESSAGE);
                mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL);
            }
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
    }
}
