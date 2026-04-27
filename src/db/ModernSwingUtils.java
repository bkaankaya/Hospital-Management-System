package db;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern Swing UI bileşenleri ve yardımcı sınıflar
 * Savunma sanayi standartlarına uygun profesyonel görünüm
 */
public class ModernSwingUtils {
    
    // Renk paleti - Profesyonel ve güvenlik odaklı
    public static final Color PRIMARY_COLOR = new Color(41, 128, 185);      // Mavi
    public static final Color SECONDARY_COLOR = new Color(52, 73, 94);      // Koyu gri
    public static final Color SUCCESS_COLOR = new Color(39, 174, 96);       // Yeşil
    public static final Color WARNING_COLOR = new Color(243, 156, 18);      // Turuncu
    public static final Color DANGER_COLOR = new Color(231, 76, 60);        // Kırmızı
    public static final Color LIGHT_GRAY = new Color(236, 240, 241);        // Açık gri
    public static final Color DARK_GRAY = new Color(44, 62, 80);           // Koyu gri
    public static final Color WHITE = Color.WHITE;
    
    // Font tanımları
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    
    /**
     * Modern buton oluşturur
     */
    public static JButton createModernButton(String text, Color backgroundColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(backgroundColor);
        button.setForeground(textColor);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover efekti
        button.addMouseListener(new MouseAdapter() {
            Color originalColor = backgroundColor;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(originalColor.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalColor);
            }
        });
        
        // Padding
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        return button;
    }
    
    /**
     * Primary buton (Ana işlemler için)
     */
    public static JButton createPrimaryButton(String text) {
        return createModernButton(text, PRIMARY_COLOR, WHITE);
    }
    
    /**
     * Secondary buton (İkincil işlemler için)
     */
    public static JButton createSecondaryButton(String text) {
        return createModernButton(text, SECONDARY_COLOR, WHITE);
    }
    
    /**
     * Success buton (Onay işlemleri için)
     */
    public static JButton createSuccessButton(String text) {
        return createModernButton(text, SUCCESS_COLOR, WHITE);
    }
    
    /**
     * Danger buton (Silme/İptal işlemleri için)
     */
    public static JButton createDangerButton(String text) {
        return createModernButton(text, DANGER_COLOR, WHITE);
    }
    
    /**
     * Modern text field oluşturur
     */
    public static JTextField createModernTextField(String placeholder) {
        JTextField textField = new JTextField();
        textField.setFont(LABEL_FONT);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_GRAY, 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Placeholder efekti
        if (placeholder != null && !placeholder.isEmpty()) {
            textField.setText(placeholder);
            textField.setForeground(Color.GRAY);
            
            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (textField.getText().equals(placeholder)) {
                        textField.setText("");
                        textField.setForeground(Color.BLACK);
                    }
                }
                
                public void focusLost(java.awt.event.FocusEvent evt) {
                    if (textField.getText().isEmpty()) {
                        textField.setText(placeholder);
                        textField.setForeground(Color.GRAY);
                    }
                }
            });
        }
        
        return textField;
    }
    
    /**
     * Modern password field oluşturur
     */
    public static JPasswordField createModernPasswordField(String placeholder) {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(LABEL_FONT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_GRAY, 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        return passwordField;
    }
    
    /**
     * Modern label oluşturur
     */
    public static JLabel createModernLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }
    
    /**
     * Başlık label'ı
     */
    public static JLabel createTitleLabel(String text) {
        return createModernLabel(text, TITLE_FONT, DARK_GRAY);
    }
    
    /**
     * Alt başlık label'ı
     */
    public static JLabel createSubtitleLabel(String text) {
        return createModernLabel(text, SUBTITLE_FONT, SECONDARY_COLOR);
    }
    
    /**
     * Modern panel oluşturur
     */
    public static JPanel createModernPanel(Color backgroundColor) {
        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return panel;
    }
    
    /**
     * Card görünümlü panel
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        return panel;
    }
    
    /**
     * Modern combo box oluşturur
     */
    public static JComboBox<String> createModernComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(LABEL_FONT);
        comboBox.setBackground(WHITE);
        comboBox.setBorder(BorderFactory.createLineBorder(LIGHT_GRAY, 2));
        return comboBox;
    }
    
    /**
     * Modern table oluşturur
     */
    public static JTable createModernTable(Object[][] data, String[] columnNames) {
        JTable table = new JTable(data, columnNames);
        table.setFont(LABEL_FONT);
        table.setRowHeight(30);
        table.setGridColor(LIGHT_GRAY);
        table.setSelectionBackground(PRIMARY_COLOR.brighter());
        table.setSelectionForeground(WHITE);
        
        // Header styling
        table.getTableHeader().setFont(BUTTON_FONT);
        table.getTableHeader().setBackground(SECONDARY_COLOR);
        table.getTableHeader().setForeground(WHITE);
        
        return table;
    }
    
    /**
     * Modern scroll pane oluşturur
     */
    public static JScrollPane createModernScrollPane(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createLineBorder(LIGHT_GRAY, 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }
    
    /**
     * Başarı mesajı gösterir
     */
    public static void showSuccessMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Başarılı", 
                                    JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Hata mesajı gösterir
     */
    public static void showErrorMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Hata", 
                                    JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Uyarı mesajı gösterir
     */
    public static void showWarningMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Uyarı", 
                                    JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Onay dialog'u gösterir
     */
    public static boolean showConfirmDialog(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, "Onay", 
                                                 JOptionPane.YES_NO_OPTION);
        return result == JOptionPane.YES_OPTION;
    }
    
    /**
     * Look and Feel'i modern yapar
     */
    public static void setModernLookAndFeel() {
        try {
            // System Look and Feel kullan (Windows'ta modern görünüm)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Özel renkler ayarla
            UIManager.put("Button.background", PRIMARY_COLOR);
            UIManager.put("Button.foreground", WHITE);
            UIManager.put("Panel.background", LIGHT_GRAY);
            
        } catch (Exception e) {
            // Varsayılan Look and Feel kullan
            System.err.println("Modern Look and Feel ayarlanamadı: " + e.getMessage());
        }
    }
}
