package db;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private Stack<String> panelHistory; // Önceki panellere geri dönebilmek için Stack

    // Panel isimleri
    public static final String LOGIN_PANEL = "LoginPanel";
    public static final String INSERT_MENU_PANEL = "InsertMenuPanel";
    public static final String INSERT_DOCTOR_PANEL = "InsertDoctorPanel";
    public static final String INSERT_PATIENT_PANEL = "InsertPatientPanel";
    public static final String DOCTOR_DASHBOARD_PANEL = "DoctorDashboardPanel";
    public static final String DOCTOR_UPDATE_PANEL = "DoctorUpdatePanel";
    public static final String CANCEL_APPOINTMENT_PANEL = "CancelAppointmentPanel";
    public static final String PATIENT_DASHBOARD_PANEL = "PatientDashboardPanel";
    public static final String PATIENT_UPDATE_PANEL = "PatientUpdatePanel";
    public static final String APPOINTMENT_PANEL = "AppointmentPanel";
    public static final String PATIENT_NAME_LIKE_PANEL = "PatientNameLikePanel";
    public static final String MEDICINE_STATS_PANEL = "MedicineStatsPanel";

    // Yeni sabitler ekleyelim
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 700;
    private static final String APP_TITLE = "Hospital Management System";

    // Tema değişimi için
    private boolean isDarkMode = false;

    public MainFrame() {
        setTitle(APP_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        panelHistory = new Stack<>();

        // Panelleri oluştur ve ekle
        mainPanel.add(new LoginPanel(this), LOGIN_PANEL);
        mainPanel.add(new InsertMenuPanel(this), INSERT_MENU_PANEL);
        mainPanel.add(new InsertDoctorPanel(this), INSERT_DOCTOR_PANEL);
        mainPanel.add(new InsertPatientPanel(this), INSERT_PATIENT_PANEL);
        mainPanel.add(new DoctorDashboardPanel(this), DOCTOR_DASHBOARD_PANEL);
        mainPanel.add(new DoctorUpdatePanel(this), DOCTOR_UPDATE_PANEL);
        mainPanel.add(new CancelAppointmentPanel(this), CANCEL_APPOINTMENT_PANEL);
        mainPanel.add(new PatientDashboardPanel(this), PATIENT_DASHBOARD_PANEL);
        mainPanel.add(new PatientUpdatePanel(this), PATIENT_UPDATE_PANEL);
        mainPanel.add(new AppointmentPanel(this), APPOINTMENT_PANEL);
        mainPanel.add(new PatientNameLikePanel(this), PATIENT_NAME_LIKE_PANEL);
        mainPanel.add(new MedicineStatsPanel(this), MEDICINE_STATS_PANEL);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        setJMenuBar(createMenuBar());

        // **Uygulama açıldığında giriş ekranını göster**
        showPanel(LOGIN_PANEL);

        // Tema değişimi için dinleyici ekleyelim
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveApplicationState();
            }
        });
    }

    /**
     * Panel değiştirme metodu (artık yeni eklenen paneller kesinlikle GUI'de
     * görünecek!)
     */
    public void showPanel(String panelName) {
        if (!panelName.equals(LOGIN_PANEL)) {
            panelHistory.push(panelName);
        }
        cardLayout.show(mainPanel, panelName);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Ortak Menü Çubuğu (Navigation)
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Navigation menüsü
        JMenu navigationMenu = new JMenu("Navigation");
        JMenuItem homeItem = new JMenuItem("🏠 Home");
        homeItem.addActionListener(e -> showPanel(LOGIN_PANEL));
        navigationMenu.add(homeItem);

        JMenuItem backItem = new JMenuItem("⬅ Back");
        backItem.addActionListener(e -> goBack());
        navigationMenu.add(backItem);

        JMenuItem exitItem = new JMenuItem("❌ Exit");
        exitItem.addActionListener(e -> System.exit(0));
        navigationMenu.add(exitItem);

        // Görünüm menüsü
        JMenu viewMenu = new JMenu("Görünüm");

        JMenuItem themeItem = new JMenuItem("🌓 Tema Değiştir");
        themeItem.addActionListener(e -> toggleTheme());
        viewMenu.add(themeItem);

        JMenuItem fontSizeItem = new JMenuItem("📝 Yazı Boyutu");
        fontSizeItem.addActionListener(e -> changeFontSize());
        viewMenu.add(fontSizeItem);

        menuBar.add(navigationMenu);
        menuBar.add(viewMenu);
        return menuBar;
    }

    /**
     * Önceki ekrana geri dönebilme özelliği
     */
    private void goBack() {
        if (!panelHistory.isEmpty()) {
            String previousPanel = panelHistory.pop();
            cardLayout.show(mainPanel, previousPanel);
            mainPanel.revalidate();
            mainPanel.repaint();
        }
    }

    /**
     * Kullanıcı giriş yaptığında otomatik olarak "Insert Doctor Paneli"ne
     * yönlendirme
     */
    public void loginSuccessful() {
        showPanel(INSERT_MENU_PANEL); // Giriş başarılı olduğunda Insert Menu panelini aç
    }

    // Yeni metodlar
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                SwingUtilities.updateComponentTreeUI(this);
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                SwingUtilities.updateComponentTreeUI(this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void changeFontSize() {
        String[] options = { "Küçük", "Orta", "Büyük" };
        int choice = JOptionPane.showOptionDialog(this,
                "Yazı boyutunu seçin:",
                "Yazı Boyutu",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);

        float newSize;
        switch (choice) {
            case 0:
                newSize = 11f;
                break;
            case 1:
                newSize = 12f;
                break;
            case 2:
                newSize = 14f;
                break;
            default:
                return;
        }

        updateFontSize(newSize);
    }

    private void updateFontSize(float size) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                Font font = UIManager.getFont(key);
                UIManager.put(key, new Font(font.getFamily(), font.getStyle(), (int) size));
            }
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void saveApplicationState() {
        // Uygulama durumunu kaydetme işlemleri buraya eklenebilir
        System.out.println("Uygulama durumu kaydediliyor...");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
