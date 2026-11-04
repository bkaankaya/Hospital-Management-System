package db;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Fetches real departments and renders the original SQL statistics without blocking Swing. */
public class MedicineStatsPanel extends JPanel {
    private final JComboBox<String> departments = new JComboBox<>();
    private final JTextArea result = new JTextArea(5, 24);
    private final JLabel status = new JLabel("Select a department.");
    private int departmentGeneration;
    private int statisticsGeneration;
    private boolean changingControls;

    public MedicineStatsPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        JPanel selector = new JPanel(new GridBagLayout());
        selector.setOpaque(false);
        DesktopStyles.row(selector, 0, "Department", departments);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(result);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        status.setForeground(DesktopStyles.MUTED);
        footer.add(status, BorderLayout.NORTH);
        JButton back = DesktopStyles.button("Back to administration", false);
        back.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
        footer.add(back, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(selector, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        add(DesktopStyles.page("Medicine statistics",
                "Department-level figures from the existing SQL assignment",
                content), BorderLayout.CENTER);
        departments.addActionListener(e -> {
            if (!changingControls) loadStatistics();
        });
    }

    public void refresh() {
        int generation = ++departmentGeneration;
        ++statisticsGeneration;
        changingControls = true;
        departments.removeAllItems();
        changingControls = false;
        result.setText("");
        status.setText("Loading departments...");
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() {
                return new DepartmentDAO().getDepartmentList();
            }
            @Override protected void done() {
                if (generation != departmentGeneration) return;
                try {
                    List<String> values = get();
                    if (values == null || values.isEmpty()) {
                        status.setText("Departments unavailable. Check MySQL.");
                        return;
                    }
                    changingControls = true;
                    for (String name : values) departments.addItem(name);
                    changingControls = false;
                    loadStatistics();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Department lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Unable to load departments.");
                } finally {
                    changingControls = false;
                }
            }
        }.execute();
    }

    private void loadStatistics() {
        String selected = (String) departments.getSelectedItem();
        final int generation = ++statisticsGeneration;
        result.setText("");
        if (selected == null) return;
        status.setText("Loading department statistics...");
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() {
                return new MedicineDAO().getStatistics(selected);
            }
            @Override protected void done() {
                if (generation != statisticsGeneration) return;
                try {
                    List<String> values = get();
                    if (values == null || values.size() < 2) {
                        status.setText("Could not read medicine statistics.");
                        return;
                    }
                    result.setText("Department: " + selected + "\n\n"
                            + "Average entries per medicine name: "
                            + (values.get(0) == null ? "0" : values.get(0)) + "\n"
                            + "Sum of medicine barcode numbers (legacy SQL metric): "
                            + (values.get(1) == null ? "0" : values.get(1)));
                    status.setText("Statistics loaded.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Statistics lookup interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Unable to load statistics.");
                }
            }
        }.execute();
    }
}
