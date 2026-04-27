package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Modern ve güvenli giriş paneli
 * Savunma sanayi standartlarına uygun tasarım
 */
public class ModernLoginPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(ModernLoginPanel.class.getName());
    private MainFrame mainFrame;
    
    // UI Components
    private JTextField ssnField;
    private JTextField nameField;
    private JTextField surnameField;
    private JPasswordField passwordField;
    private JComboBox<String> userTypeCombo;
    private JButton loginButton;
    private JLabel statusLabel;
    
    public ModernLoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        setBackground(ModernSwingUtils.LIGHT_GRAY);
        
        // User type selection
        String[] userTypes = {"Doktor", "Hasta", "Yönetici"};
        userTypeCombo = ModernSwingUtils.createModernComboBox(userTypes);
        
        // Input fields
        ssnField = ModernSwingUtils.createModernTextField("SSN (11 haneli)");
        nameField = ModernSwingUtils.createModernTextField("İsim");
        surnameField = ModernSwingUtils.createModernTextField("Soyisim");
        passwordField = ModernSwingUtils.createModernPasswordField("Şifre");
        
        // Buttons
        loginButton = ModernSwingUtils.createPrimaryButton("GİRİŞ YAP");
        
        // Status label
        statusLabel = ModernSwingUtils.createSubtitleLabel("");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Main card panel
        JPanel cardPanel = ModernSwingUtils.createCardPanel();
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setPreferredSize(new Dimension(450, 600));
        
        // Title
        JLabel titleLabel = ModernSwingUtils.createTitleLabel("HASTANE YÖNETİM SİSTEMİ");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = ModernSwingUtils.createSubtitleLabel("Güvenli Giriş");
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Logo placeholder
        JLabel logoLabel = new JLabel("🏥");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Layout components in card
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        gbc.gridx = 0; gbc.gridy = 0;
        cardPanel.add(logoLabel, gbc);
        
        gbc.gridy = 1;
        cardPanel.add(titleLabel, gbc);
        
        gbc.gridy = 2;
        cardPanel.add(subtitleLabel, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 20, 5, 20);
        cardPanel.add(ModernSwingUtils.createSubtitleLabel("Kullanıcı Tipi:"), gbc);
        
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 20, 15, 20);
        cardPanel.add(userTypeCombo, gbc);
        
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 20, 5, 20);
        cardPanel.add(ModernSwingUtils.createSubtitleLabel("SSN:"), gbc);
        
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 20, 15, 20);
        cardPanel.add(ssnField, gbc);
        
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 20, 5, 20);
        cardPanel.add(ModernSwingUtils.createSubtitleLabel("İsim:"), gbc);
        
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 20, 15, 20);
        cardPanel.add(nameField, gbc);
        
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 20, 5, 20);
        cardPanel.add(ModernSwingUtils.createSubtitleLabel("Soyisim:"), gbc);
        
        gbc.gridy = 10;
        gbc.insets = new Insets(0, 20, 15, 20);
        cardPanel.add(surnameField, gbc);
        
        gbc.gridy = 11;
        gbc.insets = new Insets(0, 20, 5, 20);
        cardPanel.add(ModernSwingUtils.createSubtitleLabel("Şifre:"), gbc);
        
        gbc.gridy = 12;
        gbc.insets = new Insets(0, 20, 20, 20);
        cardPanel.add(passwordField, gbc);
        
        gbc.gridy = 13;
        gbc.insets = new Insets(10, 20, 10, 20);
        loginButton.setPreferredSize(new Dimension(200, 40));
        cardPanel.add(loginButton, gbc);
        
        gbc.gridy = 14;
        gbc.insets = new Insets(10, 20, 20, 20);
        cardPanel.add(statusLabel, gbc);
        
        // Add card to main panel
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(cardPanel, gbc);
    }
    
    private void setupEventHandlers() {
        loginButton.addActionListener(new LoginActionListener());
        
        // Enter key support
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke("ENTER");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKeyStroke, "login");
        getActionMap().put("login", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginButton.doClick();
            }
        });
    }
    
    private class LoginActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            performLogin();
        }
    }
    
    private void performLogin() {
        // Input validation
        String ssn = ssnField.getText().trim();
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String userType = (String) userTypeCombo.getSelectedItem();
        
        // Clear status
        statusLabel.setText("");
        statusLabel.setForeground(ModernSwingUtils.SECONDARY_COLOR);
        
        // Validation
        if (ssn.isEmpty() || name.isEmpty() || surname.isEmpty() || password.isEmpty()) {
            showError("Lütfen tüm alanları doldurunuz!");
            return;
        }
        
        if (!SecurityManager.isValidSSN(ssn)) {
            showError("Geçersiz SSN formatı!");
            return;
        }
        
        // Sanitize inputs
        final String finalSsn = SecurityManager.sanitizeInput(ssn);
        final String finalName = SecurityManager.sanitizeInput(name);
        final String finalSurname = SecurityManager.sanitizeInput(surname);
        final String finalPassword = password;
        final String finalUserType = userType;
        
        // Disable login button during authentication
        loginButton.setEnabled(false);
        loginButton.setText("GİRİŞ YAPILIYOR...");
        
        // Perform authentication in background thread
        SwingWorker<Boolean, Void> loginWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return authenticateUser(finalSsn, finalName, finalSurname, finalPassword, finalUserType);
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        showSuccess("Giriş başarılı!");
                        logger.info("Kullanıcı girişi başarılı: " + finalUserType + " - " + finalSsn);
                        
                        // Navigate to appropriate dashboard
                        SwingUtilities.invokeLater(() -> {
                            switch (finalUserType) {
                                case "Doktor":
                                    mainFrame.showPanel(MainFrame.DOCTOR_DASHBOARD_PANEL);
                                    break;
                                case "Hasta":
                                    mainFrame.showPanel(MainFrame.PATIENT_DASHBOARD_PANEL);
                                    break;
                                case "Yönetici":
                                    mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL);
                                    break;
                            }
                        });
                    } else {
                        showError("Giriş bilgileri hatalı!");
                        logger.warning("Başarısız giriş denemesi: " + finalUserType + " - " + finalSsn);
                    }
                } catch (Exception ex) {
                    showError("Giriş sırasında hata oluştu!");
                    logger.severe("Giriş hatası: " + ex.getMessage());
                } finally {
                    // Re-enable login button
                    loginButton.setEnabled(true);
                    loginButton.setText("GİRİŞ YAP");
                }
            }
        };
        
        loginWorker.execute();
    }
    
    private boolean authenticateUser(String ssn, String name, String surname, String password, String userType) {
        String tableName = userType.equals("Doktor") ? "doctor" : "patient";
        String query = "SELECT password_hash, salt FROM " + tableName + 
                      " WHERE SSN = ? AND name = ? AND surname = ? AND is_active = 1";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            
            pst.setString(1, ssn);
            pst.setString(2, name);
            pst.setString(3, surname);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    
                    if (storedHash != null && salt != null) {
                        String inputHash = SecurityManager.hashPassword(password, salt);
                        return storedHash.equals(inputHash);
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Kimlik doğrulama hatası: " + e.getMessage());
        }
        
        return false;
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(ModernSwingUtils.DANGER_COLOR);
    }
    
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(ModernSwingUtils.SUCCESS_COLOR);
    }
    
    /**
     * Form alanlarını temizler
     */
    public void clearForm() {
        ssnField.setText("");
        nameField.setText("");
        surnameField.setText("");
        passwordField.setText("");
        statusLabel.setText("");
        userTypeCombo.setSelectedIndex(0);
    }
}
