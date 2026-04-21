package scm.ui.util;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * LuxuryTheme — Central design system for the SCM UI Subsystem.
 * Provides a dark premium palette, custom fonts, rounded components,
 * and consistent styling across all 10 UI components.
 */
public class LuxuryTheme {

    // ── Palette ─────────────────────────────────────────────────────────────
    public static final Color BG_DEEP      = new Color(10,  12,  20);   // deepest background
    public static final Color BG_PANEL     = new Color(16,  20,  35);   // panel background
    public static final Color BG_CARD      = new Color(22,  28,  48);   // card background
    public static final Color BG_SIDEBAR   = new Color(13,  16,  28);   // sidebar
    public static final Color BG_HOVER     = new Color(30,  38,  65);   // hover state
    public static final Color BG_SELECTED  = new Color(35,  50,  90);   // selected state
    public static final Color BG_INPUT     = new Color(20,  26,  44);   // input field

    public static final Color ACCENT_GOLD  = new Color(212, 175,  55);  // luxury gold
    public static final Color ACCENT_BLUE  = new Color( 64, 156, 255);  // primary blue
    public static final Color ACCENT_TEAL  = new Color( 32, 201, 178);  // teal
    public static final Color ACCENT_VIOLET= new Color(138,  92, 246);  // violet

    public static final Color TEXT_PRIMARY = new Color(230, 235, 250);
    public static final Color TEXT_SECOND  = new Color(140, 155, 190);
    public static final Color TEXT_MUTED   = new Color( 80,  95, 130);
    public static final Color TEXT_GOLD    = new Color(212, 175,  55);

    public static final Color SUCCESS      = new Color( 52, 199, 120);
    public static final Color WARNING      = new Color(255, 185,  50);
    public static final Color DANGER       = new Color(255,  70,  90);
    public static final Color INFO         = new Color( 64, 156, 255);

    public static final Color BORDER_SUBTLE = new Color(35, 45, 75);
    public static final Color BORDER_ACCENT = new Color(60, 80, 130);

    // ── Fonts ────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD,   22);
    public static final Font FONT_HEADING  = new Font("Segoe UI", Font.BOLD,   15);
    public static final Font FONT_SUBHEAD  = new Font("Segoe UI", Font.BOLD,   13);
    public static final Font FONT_BODY     = new Font("Segoe UI", Font.PLAIN,  13);
    public static final Font FONT_SMALL    = new Font("Segoe UI", Font.PLAIN,  11);
    public static final Font FONT_MONO     = new Font("JetBrains Mono", Font.PLAIN, 12);
    public static final Font FONT_BADGE    = new Font("Segoe UI", Font.BOLD,   10);
    public static final Font FONT_NAV      = new Font("Segoe UI", Font.PLAIN,  13);
    public static final Font FONT_NAV_SEL  = new Font("Segoe UI", Font.BOLD,   13);

    // ── Global LAF setup ─────────────────────────────────────────────────────
    public static void apply() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Panel.background",          BG_PANEL);
        UIManager.put("OptionPane.background",      BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background",          BG_CARD);
        UIManager.put("Button.foreground",          TEXT_PRIMARY);
        UIManager.put("Button.border",              BorderFactory.createEmptyBorder(8,18,8,18));
        UIManager.put("Label.foreground",           TEXT_PRIMARY);
        UIManager.put("TextField.background",       BG_INPUT);
        UIManager.put("TextField.foreground",       TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",  ACCENT_GOLD);
        UIManager.put("TextField.border",           new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_ACCENT), BorderFactory.createEmptyBorder(6,10,6,10)));
        UIManager.put("PasswordField.background",   BG_INPUT);
        UIManager.put("PasswordField.foreground",   TEXT_PRIMARY);
        UIManager.put("PasswordField.caretForeground", ACCENT_GOLD);
        UIManager.put("ComboBox.background",        BG_INPUT);
        UIManager.put("ComboBox.foreground",        TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", BG_SELECTED);
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
        UIManager.put("Table.background",           BG_PANEL);
        UIManager.put("Table.foreground",           TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground",  BG_SELECTED);
        UIManager.put("Table.selectionForeground",  TEXT_PRIMARY);
        UIManager.put("Table.gridColor",            BORDER_SUBTLE);
        UIManager.put("TableHeader.background",     BG_CARD);
        UIManager.put("TableHeader.foreground",     ACCENT_GOLD);
        UIManager.put("TableHeader.font",           FONT_SUBHEAD);
        UIManager.put("ScrollPane.background",      BG_PANEL);
        UIManager.put("ScrollBar.background",       BG_DEEP);
        UIManager.put("ScrollBar.thumb",            BG_SELECTED);
        UIManager.put("ScrollBar.track",            BG_PANEL);
        UIManager.put("TabbedPane.background",      BG_PANEL);
        UIManager.put("TabbedPane.foreground",      TEXT_SECOND);
        UIManager.put("TabbedPane.selected",        BG_SELECTED);
        UIManager.put("TabbedPane.selectedForeground", TEXT_PRIMARY);
        UIManager.put("SplitPane.background",       BG_DEEP);
        UIManager.put("Dialog.background",          BG_CARD);
        UIManager.put("ToolTip.background",         BG_CARD);
        UIManager.put("ToolTip.foreground",         TEXT_PRIMARY);
        UIManager.put("ToolTip.border",             BorderFactory.createLineBorder(BORDER_ACCENT));
        UIManager.put("TextArea.background",        BG_INPUT);
        UIManager.put("TextArea.foreground",        TEXT_PRIMARY);
        UIManager.put("TextArea.caretForeground",   ACCENT_GOLD);
        UIManager.put("CheckBox.background",        BG_PANEL);
        UIManager.put("CheckBox.foreground",        TEXT_PRIMARY);
        UIManager.put("RadioButton.background",     BG_PANEL);
        UIManager.put("RadioButton.foreground",     TEXT_PRIMARY);
        UIManager.put("Spinner.background",         BG_INPUT);
        UIManager.put("Spinner.foreground",         TEXT_PRIMARY);
    }

    // ── Component Factories ──────────────────────────────────────────────────

    /** Primary gold action button */
    public static JButton goldButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(212, 175, 55),
                    0, getHeight(), new Color(170, 130, 30));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_SUBHEAD);
        btn.setForeground(new Color(15, 15, 15));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        addHoverEffect(btn, new Color(230, 195, 70), new Color(212, 175, 55));
        return btn;
    }

    /** Primary blue action button */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(64, 156, 255),
                    0, getHeight(), new Color(40, 110, 200));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_SUBHEAD);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        return btn;
    }

    /** Danger/delete red button */
    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 70, 90),
                    0, getHeight(), new Color(200, 40, 60));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_SUBHEAD);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        return btn;
    }

    /** Ghost/outline button */
    public static JButton ghostButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_HOVER);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(BORDER_ACCENT);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth()-2, getHeight()-2, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY);
        btn.setForeground(TEXT_PRIMARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return btn;
    }

    /** Styled text field */
    public static JTextField textField(String placeholder) {
        JTextField tf = new JTextField(20);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_GOLD);
        tf.setFont(FONT_BODY);
        tf.setBorder(new CompoundBorder(
            new LineBorder(BORDER_ACCENT, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        if (placeholder != null && !placeholder.isEmpty()) {
            tf.setText(placeholder);
            tf.setForeground(TEXT_MUTED);
            tf.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    if (tf.getText().equals(placeholder)) { tf.setText(""); tf.setForeground(TEXT_PRIMARY); }
                }
                @Override public void focusLost(FocusEvent e) {
                    if (tf.getText().isEmpty()) { tf.setText(placeholder); tf.setForeground(TEXT_MUTED); }
                }
            });
        }
        return tf;
    }

    /** Styled password field */
    public static JPasswordField passwordField() {
        JPasswordField pf = new JPasswordField(20);
        pf.setBackground(BG_INPUT);
        pf.setForeground(TEXT_PRIMARY);
        pf.setCaretColor(ACCENT_GOLD);
        pf.setFont(FONT_BODY);
        pf.setBorder(new CompoundBorder(
            new LineBorder(BORDER_ACCENT, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return pf;
    }

    /** Styled text area */
    public static JTextArea textArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setBackground(BG_INPUT);
        ta.setForeground(TEXT_PRIMARY);
        ta.setCaretColor(ACCENT_GOLD);
        ta.setFont(FONT_BODY);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return ta;
    }

    /** Styled combo box */
    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
        cb.setBorder(new LineBorder(BORDER_ACCENT, 1, true));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                setBackground(sel ? BG_SELECTED : BG_INPUT);
                setForeground(TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return this;
            }
        });
        return cb;
    }

    /** KPI card panel */
    public static JPanel kpiCard(String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth()-2, getHeight()-2, 16, 16));
                // accent bar top
                g2.setColor(accent);
                g2.fillRoundRect(16, 0, 60, 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_SECOND);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 28));
        val.setForeground(accent);

        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return card;
    }

    /** Section title label */
    public static JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(TEXT_GOLD);
        return lbl;
    }

    /** Sub-heading label */
    public static JLabel subHeading(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADING);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    /** Label with secondary colour */
    public static JLabel mutedLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_SECOND);
        return lbl;
    }

    /** Styled JTable inside a scroll pane */
    public static JScrollPane styledTable(JTable table) {
        table.setBackground(BG_PANEL);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(BG_SELECTED);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_SUBTLE);
        table.setRowHeight(34);
        table.setFont(FONT_BODY);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setBackground(BG_CARD);
        table.getTableHeader().setForeground(ACCENT_GOLD);
        table.getTableHeader().setFont(FONT_SUBHEAD);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_GOLD));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? BG_SELECTED : (row % 2 == 0 ? BG_PANEL : BG_CARD));
                setForeground(sel ? TEXT_PRIMARY : TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                return this;
            }
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(BG_PANEL);
        sp.getViewport().setBackground(BG_PANEL);
        sp.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        return sp;
    }

    /** Card panel wrapper */
    public static JPanel cardPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return panel;
    }

    /** Separator line */
    public static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_SUBTLE);
        sep.setBackground(BG_PANEL);
        return sep;
    }

    /** Status badge label */
    public static JLabel statusBadge(String status) {
        JLabel badge = new JLabel(status) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FONT_BADGE);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        switch (status.toUpperCase()) {
            case "ACTIVE": case "COMPLETED": case "PAID": case "GOOD": case "BALANCED":
                badge.setBackground(new Color(52, 199, 120, 50));
                badge.setForeground(SUCCESS);
                break;
            case "PENDING": case "IN_PROGRESS": case "STAGED":
                badge.setBackground(new Color(255, 185, 50, 50));
                badge.setForeground(WARNING);
                break;
            case "INACTIVE": case "CANCELLED": case "REJECTED": case "EXPIRED": case "DAMAGED":
                badge.setBackground(new Color(255, 70, 90, 50));
                badge.setForeground(DANGER);
                break;
            default:
                badge.setBackground(new Color(64, 156, 255, 50));
                badge.setForeground(INFO);
        }
        return badge;
    }

    /** Spinner */
    public static JSpinner spinner(int min, int max, int val) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setBackground(BG_INPUT);
        sp.setForeground(TEXT_PRIMARY);
        sp.setFont(FONT_BODY);
        ((JSpinner.DefaultEditor)sp.getEditor()).getTextField().setBackground(BG_INPUT);
        ((JSpinner.DefaultEditor)sp.getEditor()).getTextField().setForeground(TEXT_PRIMARY);
        return sp;
    }

    public static JSpinner spinner(int min, int max, Object val) {
        int parsed = (val instanceof Number n) ? n.intValue() : min;
        if (parsed < min) parsed = min;
        if (parsed > max) parsed = max;
        return spinner(min, max, parsed);
    }

    private static void addHoverEffect(JButton btn, Color hoverBg, Color normalBg) {
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hoverBg); btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(normalBg); btn.repaint(); }
        });
    }

    // ── Scroll bar painting ───────────────────────────────────────────────────
    public static void styleScrollBar(JScrollBar bar) {
        bar.setBackground(BG_DEEP);
        bar.setForeground(BG_SELECTED);
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = BG_SELECTED; trackColor = BG_DEEP;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0)); b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });
    }
}
