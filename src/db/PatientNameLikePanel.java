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
                java.util.List<java.util.ArrayList<String>> results =
                        new PatientDAO().patientNameLike(stringInput.getText());
                if (results == null) {
                    textArea.setText("Database search failed.");
                } else if (results.isEmpty()) {
                    textArea.setText("No matching patients.");
                } else {
                    StringBuilder output = new StringBuilder();
                    for (java.util.List<String> row : results) {
                        for (int i = 0; i < row.size(); i++) {
                            if (i > 0) output.append(" | ");
                            output.append(row.get(i) == null ? "-" : row.get(i));
                        }
                        output.append("\n");
                    }
                    textArea.setText(output.toString());
                }
            }
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
    }
}
