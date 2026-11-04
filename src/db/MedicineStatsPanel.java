package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MedicineStatsPanel extends JPanel {
    private MainFrame mainFrame;

    public MedicineStatsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);

        JLabel title = new JLabel("SUM / AVG OF MEDICINE IDs");
        title.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        title.setBounds(28, 28, 240, 75);
        add(title);

        JLabel deptLabel = new JLabel("CHOOSE DEPARTMENT");
        deptLabel.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 18));
        deptLabel.setBounds(28, 156, 201, 89);
        add(deptLabel);

        JComboBox<String> deptCombo = new JComboBox<>();
        deptCombo.setBounds(280, 159, 258, 89);
        deptCombo.addItem("Cardiology");
        deptCombo.addItem("Neurology");
        add(deptCombo);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(92, 292, 464, 89);
        add(scrollPane);

        deptCombo.addActionListener(e -> {
            java.util.List<String> values = new MedicineDAO().getStatistics(
                    (String) deptCombo.getSelectedItem());
            if (values == null || values.size() < 2) {
                textArea.setText("Could not read medicine statistics.");
                return;
            }
            String avg = values.get(0) == null ? "0" : values.get(0);
            String sum = values.get(1) == null ? "0" : values.get(1);
            textArea.setText("Avg medicine number grouped by medicine_name: " + avg
                    + "\nSum of barcode numbers: " + sum);
        });

        JButton backButton = new JButton("Geri");
        backButton.setBounds(10, 10, 80, 30);
        add(backButton);
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
    }
}
