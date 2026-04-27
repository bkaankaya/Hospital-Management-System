package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InsertDoctorPanel extends JPanel {
    private MainFrame mainFrame;
    private JTextField inputssnd, inputnamed, inputsurnamed;
    private JTextArea inputaddressd;
    private JComboBox<String> inputgenderd, inputdnd;
    private JTextField inputdobd; // Doğum tarihi için input alanı

    public InsertDoctorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        // GridBagLayout ile düzgün bir düzen oluştur
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Daha iyi bir aralık için
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Başlık
        JLabel title = new JLabel("INSERT DOCTOR");
        title.setFont(new Font("Trebuchet MS", Font.BOLD, 18));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(title, gbc);

        // SSN Alanı
        gbc.gridwidth = 1;
        gbc.gridy++;
        add(new JLabel("SSN:"), gbc);
        inputssnd = new JTextField(15);
        gbc.gridx = 1;
        add(inputssnd, gbc);
        inputssnd.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar()) || inputssnd.getText().length() >= 11) {
                    e.consume();
                }
            }
        });

        // Name Alanı
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Name:"), gbc);
        inputnamed = new JTextField(15);
        gbc.gridx = 1;
        add(inputnamed, gbc);

        // Surname Alanı
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Surname:"), gbc);
        inputsurnamed = new JTextField(15);
        gbc.gridx = 1;
        add(inputsurnamed, gbc);

        // Gender Alanı
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Gender:"), gbc);
        inputgenderd = new JComboBox<>(new String[] { "Male", "Female", "Other" });
        gbc.gridx = 1;
        add(inputgenderd, gbc);

        // Address Alanı
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Address:"), gbc);
        inputaddressd = new JTextArea(3, 15);
        inputaddressd.setLineWrap(true);
        inputaddressd.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(inputaddressd);
        gbc.gridx = 1;
        add(scrollPane, gbc);

        // Date of Birth Alanı (Doğum Tarihi)
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Date of Birth:"), gbc);
        inputdobd = new JTextField(10);
        inputdobd.setEditable(false); // Kullanıcının manuel girmesini önlemek için
        gbc.gridx = 1;
        add(inputdobd, gbc);

        JButton datePickerButton = new JButton("Pick Date");
        gbc.gridx = 2;
        add(datePickerButton, gbc);
        datePickerButton.addActionListener(e -> showDatePicker());

        // Department Alanı
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Department:"), gbc);
        inputdnd = new JComboBox<>();
        DepartmentDAO deptDao = new DepartmentDAO();
        java.util.List<String> depts = deptDao.getDepartmentList();
        if (depts != null) {
            for (String d : depts) {
                inputdnd.addItem(d);
            }
        }
        gbc.gridx = 1;
        add(inputdnd, gbc);

        // Butonlar (ADD, Clear, Back)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton addButton = new JButton("ADD");
        addButton.addActionListener(e -> addDoctor());
        buttonPanel.add(addButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearFields());
        buttonPanel.add(clearButton);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
        buttonPanel.add(backButton);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        // Paneli yeniden çizerek eklenen bileşenlerin güncellenmesini sağla
        revalidate();
        repaint();
    }

    /**
     * JSpinner kullanarak tarih seçme işlemini sağlar.
     */
    private void showDatePicker() {
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);

        int option = JOptionPane.showOptionDialog(
                null,
                dateSpinner,
                "Select Date",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null);

        if (option == JOptionPane.OK_OPTION) {
            Date selectedDate = (Date) dateSpinner.getValue();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            inputdobd.setText(sdf.format(selectedDate));
        }
    }

    private void addDoctor() {
        if (inputssnd.getText().isEmpty() || inputnamed.getText().isEmpty() ||
                inputsurnamed.getText().isEmpty() || inputaddressd.getText().isEmpty() ||
                inputdobd.getText().isEmpty() || inputdnd.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(null, "Fill in all fields!", "Warning", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Doctor Added Successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL);
        }
    }

    private void clearFields() {
        inputssnd.setText("");
        inputnamed.setText("");
        inputsurnamed.setText("");
        inputaddressd.setText("");
        inputdobd.setText("");
        inputdnd.setSelectedIndex(0);
    }
}
