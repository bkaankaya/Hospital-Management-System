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
    public static final String PATIENT_APPOINTMENTS_PANEL = "PatientAppointmentsPanel";
    public static final String PATIENT_NAME_LIKE_PANEL = "PatientNameLikePanel";
    public static final String MEDICINE_STATS_PANEL = "MedicineStatsPanel";

    // Yeni sabitler ekleyelim
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 700;
    private static final String APP_TITLE = "Hospital Management System";

    // Tema değişimi için
    private boolean isDarkMode = false;
    private String currentPanel;
    private Integer currentPatientSsn;
    private Integer currentDoctorSsn;
    private AccessPolicy.Role currentRole = AccessPolicy.Role.NONE;
    private long lastUserInputNanos;
    private java.awt.event.AWTEventListener userInputListener;
    private javax.swing.Timer inactivityCheck;
    private InsertDoctorPanel insertDoctorPanel;
    private MedicineStatsPanel medicineStatsPanel;
    private AppointmentPanel appointmentPanel;
    private PatientAppointmentsPanel patientAppointmentsPanel;
    private CancelAppointmentPanel cancelAppointmentPanel;

    public void setCurrentPatientSsn(int ssn) {
        if (ssn <= 0) throw new IllegalArgumentException("Invalid patient ID");
        currentPatientSsn = ssn;
        currentDoctorSsn = null;
        currentRole = AccessPolicy.Role.PATIENT;
        lastUserInputNanos = System.nanoTime();
    }

    public void setCurrentDoctorSsn(int ssn) {
        if (ssn <= 0) throw new IllegalArgumentException("Invalid doctor ID");
        currentDoctorSsn = ssn;
        currentPatientSsn = null;
        currentRole = AccessPolicy.Role.DOCTOR;
        lastUserInputNanos = System.nanoTime();
    }

    public void setAdminAuthenticated() {
        currentPatientSsn = null;
        currentDoctorSsn = null;
        currentRole = AccessPolicy.Role.ADMIN;
        lastUserInputNanos = System.nanoTime();
    }

    public Integer getCurrentPatientSsn() {
        return currentPatientSsn;
    }

    public Integer getCurrentDoctorSsn() {
        return currentDoctorSsn;
    }

    public MainFrame() {
        setTitle(APP_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        panelHistory = new Stack<>();

        // Panelleri oluştur ve ekle
        mainPanel.add(new LoginPanel(this), LOGIN_PANEL);
        mainPanel.add(new InsertMenuPanel(this), INSERT_MENU_PANEL);
        insertDoctorPanel = new InsertDoctorPanel(this);
        mainPanel.add(insertDoctorPanel, INSERT_DOCTOR_PANEL);
        mainPanel.add(new InsertPatientPanel(this), INSERT_PATIENT_PANEL);
        mainPanel.add(new DoctorDashboardPanel(this), DOCTOR_DASHBOARD_PANEL);
        mainPanel.add(new DoctorUpdatePanel(this), DOCTOR_UPDATE_PANEL);
        cancelAppointmentPanel = new CancelAppointmentPanel(this);
        mainPanel.add(cancelAppointmentPanel, CANCEL_APPOINTMENT_PANEL);
        mainPanel.add(new PatientDashboardPanel(this), PATIENT_DASHBOARD_PANEL);
        mainPanel.add(new PatientUpdatePanel(this), PATIENT_UPDATE_PANEL);
        appointmentPanel = new AppointmentPanel(this);
        mainPanel.add(appointmentPanel, APPOINTMENT_PANEL);
        patientAppointmentsPanel = new PatientAppointmentsPanel(this);
        mainPanel.add(patientAppointmentsPanel, PATIENT_APPOINTMENTS_PANEL);
        mainPanel.add(new PatientNameLikePanel(this), PATIENT_NAME_LIKE_PANEL);
        medicineStatsPanel = new MedicineStatsPanel(this);
        mainPanel.add(medicineStatsPanel, MEDICINE_STATS_PANEL);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        setJMenuBar(createMenuBar());

        // **Uygulama açıldığında giriş ekranını göster**
        showPanel(LOGIN_PANEL);

        // Local workstation idle-lock. This protects the unattended UI only;
        // a trusted backend must separately enforce any production session.
        installIdleLock();

        // Tema değişimi için dinleyici ekleyelim
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveApplicationState();
            }
        });
    }

    private void installIdleLock() {
        userInputListener = event -> {
            if (currentRole == AccessPolicy.Role.NONE) return;
            Object source = event.getSource();
            if (source instanceof Component
                    && SwingUtilities.isDescendingFrom((Component) source, this)) {
                lastUserInputNanos = System.nanoTime();
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(userInputListener,
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
                        | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        inactivityCheck = new javax.swing.Timer(10000, event -> {
            if (currentRole != AccessPolicy.Role.NONE
                    && IdleSessionPolicy.isExpired(lastUserInputNanos, System.nanoTime())) {
                showPanel(LOGIN_PANEL);
                JOptionPane.showMessageDialog(this,
                        "The workstation session expired after 15 minutes without activity. Please sign in again.",
                        "Session locked", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        inactivityCheck.start();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                if (inactivityCheck != null) inactivityCheck.stop();
                if (userInputListener != null)
                    Toolkit.getDefaultToolkit().removeAWTEventListener(userInputListener);
            }
        });
    }

    /**
     * Panel değiştirme metodu (artık yeni eklenen paneller kesinlikle GUI'de
     * görünecek!)
     */
    public void showPanel(String panelName) {
        if (!AccessPolicy.canOpen(currentRole, panelName)) {
            System.err.println("Unauthorized desktop panel navigation rejected");
            return;
        }
        if (LOGIN_PANEL.equals(panelName)) {
            GatewayClient.get().logout();
            currentPatientSsn = null;
            currentDoctorSsn = null;
            currentRole = AccessPolicy.Role.NONE;
            panelHistory.clear();
        } else if (currentPanel != null && !currentPanel.equals(panelName)) {
            panelHistory.push(currentPanel);
        }
        currentPanel = panelName;
        if (INSERT_DOCTOR_PANEL.equals(panelName) && insertDoctorPanel != null) insertDoctorPanel.refreshDepartments();
        if (MEDICINE_STATS_PANEL.equals(panelName) && medicineStatsPanel != null) medicineStatsPanel.refresh();
        if (APPOINTMENT_PANEL.equals(panelName) && appointmentPanel != null) appointmentPanel.refresh();
        if (PATIENT_APPOINTMENTS_PANEL.equals(panelName) && patientAppointmentsPanel != null) patientAppointmentsPanel.refresh();
        if (CANCEL_APPOINTMENT_PANEL.equals(panelName) && cancelAppointmentPanel != null) cancelAppointmentPanel.refresh();
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
        if (panelHistory.isEmpty()) return;
        String previous = panelHistory.pop();
        if (!AccessPolicy.canOpen(currentRole, previous)) {
            System.err.println("Unauthorized navigation history entry ignored");
            return;
        }
        currentPanel = previous;
        if (LOGIN_PANEL.equals(previous)) {
            GatewayClient.get().logout();
            currentPatientSsn = null;
            currentDoctorSsn = null;
            currentRole = AccessPolicy.Role.NONE;
            panelHistory.clear();
        }
        cardLayout.show(mainPanel, previous);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Kullanıcı giriş yaptığında otomatik olarak "Insert Doctor Paneli"ne
     * yönlendirme
     */
    public void loginSuccessful() {
        // Kept for legacy callers; the insert menu requires an authenticated administrator.
        showPanel(INSERT_MENU_PANEL);
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
            ModernSwingUtils.setModernLookAndFeel();
            new MainFrame().setVisible(true);
        });
    }
}
