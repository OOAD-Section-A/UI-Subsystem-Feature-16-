package scm.ui.util;

import scm.ui.util.LuxuryTheme;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * CrudPanel — reusable CRUD-table panel base.
 * BEHAVIOURAL PATTERN — Template Method:
 *   Subclasses implement loadData(), getColumns(), toRow(), openAddDialog(), openEditDialog().
 *
 * CREATIONAL PATTERN — Builder embedded in FormBuilder inner class.
 */
public abstract class CrudPanel extends JPanel {

    protected JTable table;
    protected DefaultTableModel tableModel;
    protected JTextField searchField;
    protected JLabel recordCountLabel;

    // ── Builder inner class (Creational: Builder pattern) ────────────────────
    public static class FormBuilder {
        private final JPanel panel;
        private final GridBagConstraints gbc;
        private int row = 0;

        public FormBuilder() {
            panel = new JPanel(new GridBagLayout());
            panel.setBackground(LuxuryTheme.BG_CARD);
            panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
        }

        public FormBuilder addField(String label, JComponent field) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
            JLabel lbl = new JLabel(label);
            lbl.setFont(LuxuryTheme.FONT_SMALL);
            lbl.setForeground(LuxuryTheme.TEXT_SECOND);
            panel.add(lbl, gbc);

            gbc.gridx = 1; gbc.weightx = 0.7;
            field.setBackground(LuxuryTheme.BG_INPUT);
            if (field instanceof JTextField) {
                ((JTextField)field).setForeground(LuxuryTheme.TEXT_PRIMARY);
                ((JTextField)field).setCaretColor(LuxuryTheme.ACCENT_GOLD);
                field.setFont(LuxuryTheme.FONT_BODY);
                field.setBorder(new CompoundBorder(
                    new LineBorder(LuxuryTheme.BORDER_ACCENT, 1),
                    BorderFactory.createEmptyBorder(6,10,6,10)
                ));
            } else if (field instanceof JTextArea) {
                ((JTextArea)field).setForeground(LuxuryTheme.TEXT_PRIMARY);
                field.setFont(LuxuryTheme.FONT_BODY);
            } else if (field instanceof JComboBox) {
                ((JComboBox<?>)field).setForeground(LuxuryTheme.TEXT_PRIMARY);
                field.setFont(LuxuryTheme.FONT_BODY);
            }
            panel.add(field, gbc);
            row++;
            return this;
        }

        public FormBuilder addSeparator(String sectionTitle) {
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
            JLabel lbl = new JLabel(sectionTitle);
            lbl.setFont(LuxuryTheme.FONT_SUBHEAD);
            lbl.setForeground(LuxuryTheme.ACCENT_GOLD);
            lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LuxuryTheme.BORDER_SUBTLE));
            panel.add(lbl, gbc);
            gbc.gridwidth = 1;
            row++;
            return this;
        }

        public JPanel build() { return panel; }
    }

    // ── Panel construction (template) ────────────────────────────────────────
    protected void init(String title, String[] columns) {
        setLayout(new BorderLayout(0,0));
        setBackground(LuxuryTheme.BG_PANEL);

        // ── Header ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 12, 28));

        JLabel titleLabel = LuxuryTheme.sectionTitle(title);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(titleLabel, BorderLayout.WEST);
        header.add(titleRow, BorderLayout.NORTH);

        // ── Toolbar ───────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        searchField = LuxuryTheme.textField("Search...");
        searchField.setPreferredSize(new Dimension(140, 36));
        searchField.addActionListener(e -> applySearch());

        JButton searchBtn = LuxuryTheme.ghostButton("🔍");
        searchBtn.addActionListener(e -> applySearch());

        JButton addBtn = LuxuryTheme.goldButton("+ Add");
        addBtn.addActionListener(e -> openAddDialog());

        JButton editBtn = LuxuryTheme.primaryButton("✎ Edit");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { AppUtils.showError(this, "Select a row to edit."); return; }
            openEditDialog(row);
        });

        JButton deleteBtn = LuxuryTheme.dangerButton("✕ Delete");
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { AppUtils.showError(this, "Select a row to delete."); return; }
            if (AppUtils.confirm(this, "Delete this record? This cannot be undone.")) {
                deleteRow(row);
            }
        });

        JButton exportBtn = LuxuryTheme.ghostButton("↓ CSV");
        exportBtn.addActionListener(e -> new AppUtils.CsvExportStrategy().export(table, this));

        JButton refreshBtn = LuxuryTheme.ghostButton("↺");
        refreshBtn.addActionListener(e -> { loadDataAsync(); });

        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);
        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(deleteBtn);
        toolbar.add(exportBtn);

        header.add(toolbar, BorderLayout.CENTER);

        // ── Table ─────────────────────────────────────────────────────────
        tableModel = AppUtils.tableModel(columns);
        table = new JTable(tableModel);
        JScrollPane sp = LuxuryTheme.styledTable(table);
        LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());

        // row count label
        recordCountLabel = new JLabel("0 records");
        recordCountLabel.setFont(LuxuryTheme.FONT_SMALL);
        recordCountLabel.setForeground(LuxuryTheme.TEXT_MUTED);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(LuxuryTheme.BG_PANEL);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 28, 12, 28));
        footer.add(recordCountLabel, BorderLayout.WEST);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(LuxuryTheme.BG_PANEL);
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 28));
        tableWrapper.add(sp, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(tableWrapper, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        loadDataAsync();
    }

    // ── Template methods (subclasses must implement) ─────────────────────────
    protected abstract void loadData();
    protected abstract void openAddDialog();
    protected abstract void openEditDialog(int selectedRow);
    protected abstract void deleteRow(int selectedRow);

    // ── Search ────────────────────────────────────────────────────────────────
    protected void applySearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty() || query.equals("search...")) {
            loadDataAsync();
            return;
        }
        DefaultTableModel model = tableModel;
        for (int r = model.getRowCount()-1; r >= 0; r--) {
            boolean found = false;
            for (int c = 0; c < model.getColumnCount(); c++) {
                Object v = model.getValueAt(r, c);
                if (v != null && v.toString().toLowerCase().contains(query)) { found = true; break; }
            }
            if (!found) model.removeRow(r);
        }
        updateRecordCount();
    }

    protected void loadDataAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() { loadData(); return null; }
            @Override protected void done() { updateRecordCount(); }
        };
        worker.execute();
    }

    protected void updateRecordCount() {
        if (recordCountLabel != null)
            recordCountLabel.setText(tableModel.getRowCount() + " record(s)");
    }

    public void refreshData() {
        loadDataAsync();
    }

    // ── Dialog helper ─────────────────────────────────────────────────────────
    protected JDialog createDialog(String title, int width, int height) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(width, height);
        dialog.setMinimumSize(new Dimension(Math.max(420, width - 80), Math.max(320, height - 120)));
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(LuxuryTheme.BG_CARD);
        dialog.setResizable(true);
        return dialog;
    }

    protected void addDialogButtons(JDialog dialog, JPanel content, Runnable onSave) {
        dialog.getContentPane().setLayout(new BorderLayout());

        JScrollPane contentScroll = new JScrollPane(content);
        contentScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        contentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        LuxuryTheme.styleScrollBar(contentScroll.getVerticalScrollBar());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setBackground(LuxuryTheme.BG_CARD);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 16, 20));

        JButton cancel = LuxuryTheme.ghostButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JButton save = LuxuryTheme.goldButton("Save");
        save.addActionListener(e -> onSave.run());

        btnPanel.add(cancel);
        btnPanel.add(save);

        dialog.add(contentScroll, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
    }

    // Alias
    private static class CompoundBorder extends javax.swing.border.CompoundBorder {
        CompoundBorder(javax.swing.border.Border out, javax.swing.border.Border in) { super(out, in); }
    }
}
