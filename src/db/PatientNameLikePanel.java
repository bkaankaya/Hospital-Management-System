package db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Admin-only patient search with a readable sortable JTable, not raw text output. */
public class PatientNameLikePanel extends JPanel {
    private final JTextField query = DesktopStyles.textField("Search patient by name or surname");
    private final JButton search = DesktopStyles.button("Search patients", true);
    private final JLabel status = new JLabel("Enter a name or surname to search.");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Patient", "Date", "Time", "Doctor", "Department"}, 0) {
        @Override public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public PatientNameLikePanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(query, BorderLayout.CENTER);
        top.add(search, BorderLayout.EAST);
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(680, 240));
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        status.setForeground(DesktopStyles.MUTED);
        footer.add(status, BorderLayout.NORTH);
        JButton back = DesktopStyles.button("Back to administration", false);
        back.addActionListener(e -> mainFrame.showPanel(MainFrame.INSERT_MENU_PANEL));
        footer.add(back, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        add(DesktopStyles.page("Patient search",
                "View locally stored patient names and appointment summaries",
                content), BorderLayout.CENTER);
        query.addActionListener(e -> performSearch());
        search.addActionListener(e -> performSearch());
    }

    private void performSearch() {
        String term = query.getText().trim();
        if (term.isEmpty()) {
            status.setText("Please enter a name or surname.");
            return;
        }
        model.setRowCount(0);
        search.setEnabled(false);
        status.setText("Searching records...");
        new SwingWorker<List<ArrayList<String>>, Void>() {
            @Override protected List<ArrayList<String>> doInBackground() {
                return new PatientDAO().patientNameLike(term);
            }
            @Override protected void done() {
                search.setEnabled(true);
                try {
                    List<ArrayList<String>> rows = get();
                    if (rows == null) {
                        status.setText("MySQL search failed.");
                        return;
                    }
                    for (List<String> row : rows) {
                        Object[] values = new Object[5];
                        for (int i = 0; i < values.length; i++) {
                            String value = i < row.size() ? row.get(i) : null;
                            values[i] = value == null ? "—" : value;
                        }
                        model.addRow(values);
                    }
                    status.setText(rows.size() + " matching record(s).");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Search interrupted.");
                } catch (ExecutionException ex) {
                    status.setText("Could not search records.");
                }
            }
        }.execute();
    }
}
