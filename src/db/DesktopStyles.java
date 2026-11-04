package db;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** Shared responsive Swing styling; no third-party UI framework is required. */
public final class DesktopStyles {
    public static final Color CANVAS = new Color(241, 246, 252);
    public static final Color INK = new Color(28, 45, 69);
    public static final Color MUTED = new Color(93, 111, 135);
    public static final Color BLUE = new Color(32, 104, 191);
    public static final Color LINE = new Color(211, 223, 236);

    private DesktopStyles() { }

    public static JPanel page(String title, String subtitle, JComponent body) {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(CANVAS);
        root.setBorder(BorderFactory.createEmptyBorder(28, 30, 28, 30));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel headingLabel = new JLabel(title);
        headingLabel.setFont(new Font("SansSerif", Font.BOLD, 27));
        headingLabel.setForeground(INK);
        headingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(headingLabel);
        heading.add(Box.createVerticalStrut(8));
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(subtitleLabel);
        root.add(heading, BorderLayout.NORTH);

        JPanel centered = new JPanel(new GridBagLayout());
        centered.setOpaque(false);
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        Border edge = BorderFactory.createLineBorder(LINE);
        card.setBorder(BorderFactory.createCompoundBorder(edge,
                BorderFactory.createEmptyBorder(24, 26, 24, 26)));
        card.add(body, BorderLayout.CENTER);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        centered.add(card, c);
        root.add(centered, BorderLayout.CENTER);
        return root;
    }

    public static JButton button(String label, boolean primary) {
        JButton button = new JButton(label);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(primary ? BLUE : new Color(231, 238, 247));
        button.setForeground(primary ? Color.WHITE : INK);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getAccessibleContext().setAccessibleName(label);
        return button;
    }

    public static JTextField textField(String accessibleName) {
        JTextField field = new JTextField(20);
        styleInput(field, accessibleName);
        return field;
    }

    public static void styleInput(JTextField field, String accessibleName) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(240, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        field.getAccessibleContext().setAccessibleName(accessibleName);
    }

    public static void row(JPanel form, int index, String label, JComponent input) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = index;
        left.insets = new Insets(8, 2, 8, 14);
        left.anchor = GridBagConstraints.LINE_START;
        JLabel caption = new JLabel(label);
        caption.setForeground(INK);
        caption.setFont(new Font("SansSerif", Font.BOLD, 13));
        caption.setLabelFor(input);
        form.add(caption, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = index;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(8, 0, 8, 2);
        form.add(input, right);
    }

    public static JPanel actionList(String... descriptions) {
        JPanel actions = new JPanel(new GridLayout(0, 1, 12, 12));
        actions.setOpaque(false);
        return actions;
    }
}
