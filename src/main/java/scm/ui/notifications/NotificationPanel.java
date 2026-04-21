package scm.ui.notifications;

import scm.ui.model.*;
import scm.ui.patterns.EventBus;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * C-09 — Notification & Exception Handling
 * Tabs: Notifications | System Exceptions | Audit Log
 *
 * Inputs:  notificationType, notificationMessage, exceptionDetails, subsystemCode
 * Outputs: notification list, exception flagging table, audit trail viewer
 */
public class NotificationPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();
    private ExceptionCrudPanel exceptionCrudPanel;

    // Badge callback so MainFrame can update sidebar
    public interface BadgeCallback { void onCountChange(int unread); }
    private BadgeCallback badgeCallback;

    public NotificationPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);
        buildUI();
        subscribeEvents();
    }

    public void setBadgeCallback(BadgeCallback cb) { this.badgeCallback = cb; }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("Notifications  &  Exception Handling"), BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        tabs.addTab("🔔 Notifications",    new NotificationCrudPanel());
        exceptionCrudPanel = new ExceptionCrudPanel();
        tabs.addTab("⚠  Exceptions",       exceptionCrudPanel);
        tabs.addTab("📋 Audit Log",        new AuditLogPanel());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private void subscribeEvents() {
        EventBus.getInstance().subscribe(EventBus.Event.NOTIFICATION_RECEIVED, payload ->
            SwingUtilities.invokeLater(() -> {
                if (badgeCallback != null) badgeCallback.onCountChange(facade.getUnreadNotificationCount());
            })
        );
        EventBus.getInstance().subscribe(EventBus.Event.EXCEPTION_RAISED, payload ->
            SwingUtilities.invokeLater(() -> {
                if (exceptionCrudPanel != null) {
                    exceptionCrudPanel.refreshExceptions();
                }
                JOptionPane.showMessageDialog(this,
                    "⚠  System Exception Raised\n" + (payload != null ? payload.toString() : ""),
                    "Exception Alert", JOptionPane.WARNING_MESSAGE);
            })
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Notifications
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class NotificationCrudPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;

        NotificationCrudPanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(LuxuryTheme.BG_PANEL);

            // ── Toolbar ───────────────────────────────────────────────
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            toolbar.setBackground(LuxuryTheme.BG_PANEL);
            toolbar.setBorder(BorderFactory.createEmptyBorder(16, 28, 0, 28));

            JButton refreshBtn  = LuxuryTheme.primaryButton("↻ Refresh");
            JButton markAllBtn  = LuxuryTheme.goldButton("✓ Mark All Read");
            JButton addBtn      = LuxuryTheme.primaryButton("+ New Notification");
            JButton exportBtn   = LuxuryTheme.ghostButton("↓ Export CSV");

            refreshBtn.addActionListener(e -> loadData());
            markAllBtn.addActionListener(e -> { facade.markAllNotificationsRead(); loadData(); });
            addBtn.addActionListener(e -> openAddDialog());
            exportBtn.addActionListener(e -> new AppUtils.CsvExportStrategy().export(table, this));

            toolbar.add(refreshBtn); toolbar.add(markAllBtn); toolbar.add(addBtn); toolbar.add(exportBtn);

            // ── Table ─────────────────────────────────────────────────
            model = AppUtils.tableModel(new String[]{"ID","Type","Message","Is Read","Created At"});
            table = new JTable(model);
            JScrollPane sp = LuxuryTheme.styledTable(table);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());

            // mark-read on row double-click
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = table.getSelectedRow();
                        if (row >= 0) {
                            Object idVal = model.getValueAt(row, 0);
                            if (idVal != null) {
                                facade.markNotificationRead((int)(Object) idVal);
                                loadData();
                            }
                        }
                    }
                }
            });

            JPanel tableWrap = new JPanel(new BorderLayout());
            tableWrap.setBackground(LuxuryTheme.BG_PANEL);
            tableWrap.setBorder(BorderFactory.createEmptyBorder(10, 28, 16, 28));
            tableWrap.add(sp, BorderLayout.CENTER);

            JLabel hint = LuxuryTheme.mutedLabel("Double-click a row to mark it as read.");
            hint.setBorder(BorderFactory.createEmptyBorder(6, 28, 4, 28));

            add(toolbar, BorderLayout.NORTH);
            add(tableWrap, BorderLayout.CENTER);
            add(hint, BorderLayout.SOUTH);

            loadData();
        }

        void loadData() {
            SwingWorker<Void, Void> w = new SwingWorker<>() {
                @Override protected Void doInBackground() {
                    List<UINotification> list = facade.getAllNotifications();
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (UINotification n : list) {
                            model.addRow(new Object[]{
                                n.getNotificationId(),
                                n.getNotificationType(),
                                n.getNotificationMessage(),
                                n.isRead() ? "✓ Read" : "● Unread",
                                n.getCreatedAt()
                            });
                        }
                        if (badgeCallback != null)
                            badgeCallback.onCountChange(facade.getUnreadNotificationCount());
                    });
                    return null;
                }
            };
            w.execute();
        }

        void openAddDialog() {
            JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Send Notification", true);
            d.setSize(480, 300);
            d.setLocationRelativeTo(this);
            d.getContentPane().setBackground(LuxuryTheme.BG_CARD);
            d.setLayout(new BorderLayout());

            JComboBox<String> typeCB = LuxuryTheme.comboBox(new String[]{
                "STOCK_ALERT","ORDER_UPDATE","SHIPMENT_UPDATE","SYSTEM","PRICING_CHANGE","APPROVAL_REQUIRED"
            });
            JTextArea msgTA = LuxuryTheme.textArea(5, 36);
            JScrollPane msgScroll = new JScrollPane(msgTA);

            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("New Notification")
                .addField("Type *", typeCB)
                .addField("Message *", msgScroll)
                .build();

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            btnPanel.setBackground(LuxuryTheme.BG_CARD);
            btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 16, 20));
            JButton cancel = LuxuryTheme.ghostButton("Cancel");
            JButton send = LuxuryTheme.goldButton("Send");
            cancel.addActionListener(ev -> d.dispose());
            send.addActionListener(ev -> {
                String msg = msgTA.getText().trim();
                if (msg.isEmpty()) { AppUtils.showError(d, "Message cannot be empty."); return; }
                facade.addNotification((String) typeCB.getSelectedItem(), msg);
                d.dispose(); loadData();
            });
            btnPanel.add(cancel); btnPanel.add(send);

            d.add(form, BorderLayout.CENTER);
            d.add(btnPanel, BorderLayout.SOUTH);
            d.setVisible(true);
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // System Exceptions
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ExceptionCrudPanel extends CrudPanel {
        ExceptionCrudPanel() {
            init("System Exceptions", new String[]{
                "Exception ID","Subsystem","Severity","Message","Status","Raised By","Created At"
            });
        }

        void refreshExceptions() {
            loadDataAsync();
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<SubsystemException> list = facade.getAllExceptions();
            SwingUtilities.invokeLater(() -> {
                for (SubsystemException e : list) {
                    tableModel.addRow(new Object[]{
                        e.getExceptionId(), e.getSubsystemCode(), e.getSeverityLevel(),
                        e.getExceptionMessage(), e.getStatus(),
                        e.getRaisedBy(), e.getCreatedAt()
                    });
                }
                updateRecordCount();
            });
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Log Exception", 520, 420);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("EXC"));
            JComboBox<String> subCB = LuxuryTheme.comboBox(new String[]{
                "PRICING","WAREHOUSE","ORDERS","LOGISTICS","FORECASTING","AUTH","SETTINGS"
            });
            JComboBox<String> sevCB = LuxuryTheme.comboBox(new String[]{"LOW","MEDIUM","HIGH","CRITICAL"});
            JTextArea msgTA  = LuxuryTheme.textArea(4, 36);
            JTextField byF   = LuxuryTheme.textField(
                facade.getCurrentUser() != null ? facade.getCurrentUser().getUsername() : "SYSTEM"
            );

            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Exception Details")
                .addField("Exception ID *", idF)
                .addField("Subsystem Code *", subCB)
                .addField("Severity *", sevCB)
                .addField("Message *", new JScrollPane(msgTA))
                .addField("Raised By", byF)
                .build();

            addDialogButtons(d, form, () -> {
                SubsystemException ex = new SubsystemException();
                ex.setExceptionId(idF.getText().trim());
                ex.setSubsystemCode((String) subCB.getSelectedItem());
                ex.setSeverityLevel((String) sevCB.getSelectedItem());
                ex.setExceptionMessage(msgTA.getText().trim());
                ex.setStatus("OPEN");
                ex.setRaisedBy(byF.getText().trim());
                int r = facade.createException(ex);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to log exception.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            String currentStatus = (String) tableModel.getValueAt(row, 4);
            if ("RESOLVED".equals(currentStatus)) {
                AppUtils.showError(this, "This exception is already resolved.");
                return;
            }
            if (AppUtils.confirm(this, "Mark exception " + id + " as RESOLVED?")) {
                facade.resolveException(id);
                loadDataAsync();
            }
        }

        @Override protected void deleteRow(int row) {
            facade.deleteException((String) tableModel.getValueAt(row, 0));
            loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Audit Log Viewer (read-only)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class AuditLogPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;
        private JSpinner limitSp;

        AuditLogPanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(LuxuryTheme.BG_PANEL);

            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            toolbar.setBackground(LuxuryTheme.BG_PANEL);
            toolbar.setBorder(BorderFactory.createEmptyBorder(16, 28, 0, 28));
            toolbar.add(LuxuryTheme.mutedLabel("Records:"));
            limitSp = LuxuryTheme.spinner(10, 10000, 200);
            limitSp.setPreferredSize(new Dimension(80, 34));
            JButton loadBtn = LuxuryTheme.primaryButton("Load");
            JButton expBtn  = LuxuryTheme.ghostButton("↓ Export CSV");
            loadBtn.addActionListener(e -> loadAuditLogs());
            expBtn.addActionListener(e -> new AppUtils.CsvExportStrategy().export(table, this));
            toolbar.add(limitSp); toolbar.add(loadBtn); toolbar.add(expBtn);

            // Read-only notice
            JLabel notice = new JLabel("  🔒  Audit log is read-only. Records cannot be modified or deleted.");
            notice.setFont(LuxuryTheme.FONT_SMALL);
            notice.setForeground(LuxuryTheme.WARNING);
            notice.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 28));

            model = AppUtils.tableModel(new String[]{"Log ID","Action User","Module","Action Description","Timestamp"});
            table = new JTable(model);
            JScrollPane sp = LuxuryTheme.styledTable(table);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());

            JPanel tableWrap = new JPanel(new BorderLayout());
            tableWrap.setBackground(LuxuryTheme.BG_PANEL);
            tableWrap.setBorder(BorderFactory.createEmptyBorder(10, 28, 16, 28));
            tableWrap.add(sp, BorderLayout.CENTER);

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setBackground(LuxuryTheme.BG_PANEL);
            topBar.add(toolbar, BorderLayout.NORTH);
            topBar.add(notice, BorderLayout.SOUTH);

            add(topBar, BorderLayout.NORTH);
            add(tableWrap, BorderLayout.CENTER);

            loadAuditLogs();
        }

        private void loadAuditLogs() {
            int limit = (int) limitSp.getValue();
            SwingWorker<Void, Void> w = new SwingWorker<>() {
                @Override protected Void doInBackground() {
                    List<AuditLog> logs = facade.getAuditLogs(limit);
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (AuditLog l : logs)
                            model.addRow(new Object[]{l.getLogId(), l.getActionUser(), l.getModuleName(), l.getActionDescription(), l.getLogTimestamp()});
                    });
                    return null;
                }
            };
            w.execute();
        }
    }
}
