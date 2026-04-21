package scm.ui.util;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * AppUtils — common utility methods used across all UI panels.
 * BEHAVIOURAL PATTERN — Strategy is used for export (CSV/PDF).
 */
public class AppUtils {

    private AppUtils() {}

    // ── ID generation ────────────────────────────────────────────────────────
    public static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ── Password hashing (SHA-256 — simple; bcrypt in production) ───────────
    public static String hashPassword(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plainText.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return plainText; // fallback
        }
    }

    // ── Date formatting ──────────────────────────────────────────────────────
    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    // ── Export strategies ────────────────────────────────────────────────────

    public interface ExportStrategy {
        void export(JTable table, Component parent);
    }

    public static class CsvExportStrategy implements ExportStrategy {
        @Override
        public void export(JTable table, Component parent) {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("export_" + today() + ".csv"));
            if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
            try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
                TableModel model = table.getModel();
                // Headers
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < model.getColumnCount(); c++) {
                    sb.append('"').append(model.getColumnName(c)).append('"');
                    if (c < model.getColumnCount()-1) sb.append(',');
                }
                pw.println(sb.toString());
                // Rows
                for (int r = 0; r < model.getRowCount(); r++) {
                    sb = new StringBuilder();
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        Object v = model.getValueAt(r, c);
                        sb.append('"').append(v != null ? v.toString().replace("\"","\"\"") : "").append('"');
                        if (c < model.getColumnCount()-1) sb.append(',');
                    }
                    pw.println(sb.toString());
                }
                showSuccess(parent, "Exported to CSV: " + fc.getSelectedFile().getName());
            } catch (IOException e) {
                showError(parent, "CSV export failed: " + e.getMessage());
            }
        }
    }

    public static class TxtExportStrategy implements ExportStrategy {
        @Override
        public void export(JTable table, Component parent) {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("export_" + today() + ".txt"));
            if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
            try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
                TableModel model = table.getModel();
                // widths
                int[] widths = new int[model.getColumnCount()];
                for (int c = 0; c < model.getColumnCount(); c++) widths[c] = model.getColumnName(c).length();
                for (int r = 0; r < model.getRowCount(); r++)
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        Object v = model.getValueAt(r, c);
                        if (v != null) widths[c] = Math.max(widths[c], v.toString().length());
                    }
                String fmt = ""; for (int w : widths) fmt += "%-"+(w+2)+"s";
                Object[] headers = new Object[model.getColumnCount()];
                for (int c = 0; c < model.getColumnCount(); c++) headers[c] = model.getColumnName(c);
                pw.println(String.format(fmt, headers));
                pw.println("-".repeat(200));
                for (int r = 0; r < model.getRowCount(); r++) {
                    Object[] row = new Object[model.getColumnCount()];
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        Object v = model.getValueAt(r, c); row[c] = v != null ? v : "";
                    }
                    pw.println(String.format(fmt, row));
                }
                showSuccess(parent, "Exported to TXT: " + fc.getSelectedFile().getName());
            } catch (IOException e) {
                showError(parent, "TXT export failed: " + e.getMessage());
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static String prompt(Component parent, String message, String defaultValue) {
        return JOptionPane.showInputDialog(parent, message, defaultValue);
    }

    // ── Table model helper ───────────────────────────────────────────────────
    public static javax.swing.table.DefaultTableModel tableModel(String[] columns) {
        return new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // ── Format currency ──────────────────────────────────────────────────────
    public static String currency(double amount) {
        return String.format("₹ %,.2f", amount);
    }

    public static String percent(double pct) {
        return String.format("%.1f%%", pct);
    }
}
