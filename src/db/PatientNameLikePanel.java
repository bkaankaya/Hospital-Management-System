package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PatientNameLikePanel extends JPanel {
    private MainFrame mainFrame;

    public PatientNameLikePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JLabel title = new JLabel("GET INFORMATION");
        title.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        title.setBounds(25, 11, 207, 33);
        add(title);

        JLabel inputLabel = new JLabel("Insert String");
        inputLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        inputLabel.setBounds(25, 73, 143, 43);
        add(inputLabel);

        JTextField stringInput = new JTextField();
        stringInput.setBounds(200, 73, 261, 43);
        add(stringInput);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(58, 216, 518, 201);
        add(scrollPane);

        JButton submitButton = new JButton("SUBMIT");
        submitButton.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        submitButton.setBounds(238, 151, 171, 43);
        add(submitButton);
        submitButton.addActionListener(e -> {
            if (stringInput.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Fill in all the blanks to continue!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                // Örnek veri; gerçek veri DAO’dan alınmalıdır.
                String dummyResult = "Patient1\tData1\tData2\nPatient2\tData3\tData4";
                textArea.setText(dummyResult);
            }
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL));
    }
}
