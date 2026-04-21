package scm.ui.settings;

import scm.ui.exceptions.UIAuthExceptionSource;
import scm.ui.model.*;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * C-10 — User & System Settings
 * Tabs: User Management | My Profile | Theme & UI | System Configuration
 *
 * Inputs:  userId, roleAssignment, passwordChangeRequest, notificationPreferences, systemConfigKey
 * Outputs: user list CRUD, profile update form, theme switch, config key/value editor
 */
public class SettingsPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("User  &  System Settings"), BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        tabs.addTab("👥 User Management", new UserManagementPanel());
        tabs.addTab("👤 My Profile",      new ProfilePanel());
        tabs.addTab("🎨 Theme & UI",      new ThemePanel());
        tabs.addTab("⚙  System Config",   new SystemConfigPanel());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // User Management CRUD (Admin only)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class UserManagementPanel extends CrudPanel {
        UserManagementPanel() {
            init("Users", new String[]{
                "User ID","Username","Display Name","Email","Role","Status","Last Login","Account Locked"
            });
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<UIUser> users = facade.getAllUsers();
            SwingUtilities.invokeLater(() -> {
                for (UIUser u : users) {
                    tableModel.addRow(new Object[]{
                        u.getUserId(), u.getUsername(), u.getDisplayName(), u.getEmail(),
                        u.getUserRole(), u.getStatus(), u.getLastLogin(),
                        u.isAccountLocked() ? "🔒 Yes" : "No"
                    });
                }
                updateRecordCount();
            });
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Create User", 520, 500);
            JTextField usernameF    = LuxuryTheme.textField("johndoe");
            JTextField displayNameF = LuxuryTheme.textField("John Doe");
            JTextField emailF       = LuxuryTheme.textField("john@company.com");
            JPasswordField passF    = LuxuryTheme.passwordField();
            JComboBox<String> roleCB = LuxuryTheme.comboBox(new String[]{
                "ADMIN","MANAGER","WAREHOUSE_STAFF","SALES_REP","CASHIER","ANALYST","DRIVER"
            });
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"ACTIVE","INACTIVE","SUSPENDED"});

            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("New User Details")
                .addField("Username *", usernameF)
                .addField("Display Name *", displayNameF)
                .addField("Email *", emailF)
                .addField("Password *", passF)
                .addField("Role *", roleCB)
                .addField("Status", stCB)
                .build();

            addDialogButtons(d, form, () -> {
                String password = new String(passF.getPassword());
                if (password.length() < 8) { AppUtils.showError(d, "Password must be at least 8 characters."); return; }
                UIUser u = new UIUser();
                u.setUsername(usernameF.getText().trim());
                u.setDisplayName(displayNameF.getText().trim());
                u.setEmail(emailF.getText().trim());
                u.setPasswordHash(AppUtils.hashPassword(password));
                u.setUserRole((String) roleCB.getSelectedItem());
                u.setStatus((String) stCB.getSelectedItem());
                long id = facade.createUser(u);
                if (id > 0) { d.dispose(); loadDataAsync(); AppUtils.showSuccess(d, "User created."); }
                else AppUtils.showError(d, "Failed. Username or email may already exist.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            int userId = (int) tableModel.getValueAt(row, 0);
            UIUser u = facade.getAllUsers().stream().filter(usr -> usr.getUserId() == userId).findFirst().orElse(null);
            if (u == null) return;

            JDialog d = createDialog("Edit User: " + u.getUsername(), 520, 460);
            JTextField displayNameF = LuxuryTheme.textField(u.getDisplayName());
            JTextField emailF       = LuxuryTheme.textField(u.getEmail());
            JPasswordField newPassF = LuxuryTheme.passwordField();
            JComboBox<String> roleCB = LuxuryTheme.comboBox(new String[]{
                "ADMIN","MANAGER","WAREHOUSE_STAFF","SALES_REP","CASHIER","ANALYST","DRIVER"
            });
            roleCB.setSelectedItem(u.getUserRole());
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"ACTIVE","INACTIVE","SUSPENDED"});
            stCB.setSelectedItem(u.getStatus());
            JCheckBox unlockChk = new JCheckBox("Unlock Account");
            unlockChk.setOpaque(false); unlockChk.setForeground(LuxuryTheme.TEXT_PRIMARY);
            unlockChk.setEnabled(u.isAccountLocked());

            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Edit User: " + u.getUsername())
                .addField("Display Name", displayNameF)
                .addField("Email", emailF)
                .addField("New Password (blank = keep)", newPassF)
                .addField("Role", roleCB)
                .addField("Status", stCB)
                .addField("Account Actions", unlockChk)
                .build();

            addDialogButtons(d, form, () -> {
                u.setDisplayName(displayNameF.getText().trim());
                u.setEmail(emailF.getText().trim());
                String newPass = new String(newPassF.getPassword());
                if (!newPass.isEmpty()) {
                    if (newPass.length() < 8) { AppUtils.showError(d, "Password must be at least 8 characters."); return; }
                    u.setPasswordHash(AppUtils.hashPassword(newPass));
                }
                u.setUserRole((String) roleCB.getSelectedItem());
                u.setStatus((String) stCB.getSelectedItem());
                if (unlockChk.isSelected()) { u.setAccountLocked(false); u.setLoginAttemptCount(0); }
                int r = facade.updateUser(u);
                if (r > 0) {
                    d.dispose();
                    loadDataAsync();
                } else {
                    UIAuthExceptionSource.getInstance().firePermissionError(
                        257,
                        String.valueOf(u.getUserId()),
                        "ROLE_ASSIGN",
                        "DB update returned 0 rows for userId=" + u.getUserId()
                    );
                    AppUtils.showError(d, "Update failed.");
                }
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) {
            UIUser current = facade.getCurrentUser();
            int userId = (int) tableModel.getValueAt(row, 0);
            if (current != null && current.getUserId() == userId) {
                AppUtils.showError(this, "You cannot delete your own account.");
                return;
            }
            facade.deleteUser(userId);
            loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // My Profile
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ProfilePanel extends JPanel {
        ProfilePanel() {
            setLayout(new GridBagLayout());
            setBackground(LuxuryTheme.BG_PANEL);

            UIUser me = facade.getCurrentUser();
            if (me == null) {
                add(LuxuryTheme.mutedLabel("Not logged in."));
                return;
            }

            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(LuxuryTheme.BG_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(LuxuryTheme.BORDER_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(30, 36, 30, 36)
            ));
            card.setPreferredSize(new Dimension(520, 420));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(8, 0, 8, 0);

            // Avatar
            JLabel avatar = new JLabel(String.valueOf(me.getDisplayName().charAt(0)).toUpperCase()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(LuxuryTheme.ACCENT_GOLD);
                    g2.fillOval(0, 0, 64, 64);
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 30));
                    g2.drawString(getText(), 18, 44);
                    g2.dispose();
                }
            };
            avatar.setPreferredSize(new Dimension(64, 64));
            avatar.setMinimumSize(new Dimension(64, 64));

            JLabel nameLabel = LuxuryTheme.sectionTitle(me.getDisplayName());
            JLabel roleLabel = LuxuryTheme.mutedLabel("Role: " + me.getUserRole());
            JLabel emailLabel = LuxuryTheme.mutedLabel("Email: " + me.getEmail());

            JSeparator sep = LuxuryTheme.separator();

            JTextField displayNameF = LuxuryTheme.textField(me.getDisplayName());
            JTextField emailF       = LuxuryTheme.textField(me.getEmail());
            JPasswordField oldPassF = LuxuryTheme.passwordField();
            JPasswordField newPassF = LuxuryTheme.passwordField();
            JPasswordField confirmF = LuxuryTheme.passwordField();

            JButton saveBtn = LuxuryTheme.goldButton("  Save Changes  ");
            saveBtn.addActionListener(e -> {
                String newPass = new String(newPassF.getPassword());
                String confirm = new String(confirmF.getPassword());
                if (!newPass.isEmpty()) {
                    if (newPass.length() < 8) { AppUtils.showError(card, "Password must be at least 8 characters."); return; }
                    if (!newPass.equals(confirm)) { AppUtils.showError(card, "Passwords do not match."); return; }
                    String oldHash = AppUtils.hashPassword(new String(oldPassF.getPassword()));
                    if (!oldHash.equals(me.getPasswordHash())) { AppUtils.showError(card, "Current password is incorrect."); return; }
                    me.setPasswordHash(AppUtils.hashPassword(newPass));
                }
                me.setDisplayName(displayNameF.getText().trim());
                me.setEmail(emailF.getText().trim());
                int r = facade.updateUser(me);
                if (r > 0) {
                    AppUtils.showSuccess(card, "Profile updated.");
                } else {
                    UIAuthExceptionSource.getInstance().firePermissionError(
                        258,
                        String.valueOf(me.getUserId()),
                        "SETTINGS_SAVE",
                        "DB update returned 0 rows for userId=" + me.getUserId()
                    );
                    AppUtils.showError(card, "Update failed.");
                }
            });

            JPanel avatarRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
            avatarRow.setOpaque(false);
            avatarRow.add(avatar);
            JPanel nameInfo = new JPanel();
            nameInfo.setLayout(new BoxLayout(nameInfo, BoxLayout.Y_AXIS));
            nameInfo.setOpaque(false);
            nameInfo.add(nameLabel); nameInfo.add(Box.createVerticalStrut(4)); nameInfo.add(roleLabel); nameInfo.add(emailLabel);
            avatarRow.add(nameInfo);

            CrudPanel.FormBuilder fb = new CrudPanel.FormBuilder();
            JPanel form = fb
                .addSeparator("Update Profile")
                .addField("Display Name", displayNameF)
                .addField("Email", emailF)
                .addSeparator("Change Password  (leave blank to keep current)")
                .addField("Current Password", oldPassF)
                .addField("New Password", newPassF)
                .addField("Confirm Password", confirmF)
                .build();

            card.add(avatarRow, gbc);
            card.add(Box.createVerticalStrut(8), gbc);
            card.add(sep, gbc);
            card.add(Box.createVerticalStrut(4), gbc);
            card.add(form, gbc);
            card.add(Box.createVerticalStrut(8), gbc);
            card.add(saveBtn, gbc);

            add(card);
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Theme & UI
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ThemePanel extends JPanel {
        ThemePanel() {
            setLayout(new GridBagLayout());
            setBackground(LuxuryTheme.BG_PANEL);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(LuxuryTheme.BG_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(LuxuryTheme.BORDER_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(30, 36, 30, 36)
            ));
            card.setPreferredSize(new Dimension(500, 360));

            JLabel title = LuxuryTheme.subHeading("Theme & Display Settings");
            title.setAlignmentX(LEFT_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            // Colour swatches
            JLabel swatchTitle = LuxuryTheme.mutedLabel("Current Accent Colour");
            swatchTitle.setAlignmentX(LEFT_ALIGNMENT);
            card.add(swatchTitle);
            card.add(Box.createVerticalStrut(10));

            JPanel swatchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            swatchRow.setOpaque(false);
            String[] swatchNames = {"Gold", "Blue", "Teal", "Violet"};
            Color[] swatchColors = {LuxuryTheme.ACCENT_GOLD, LuxuryTheme.ACCENT_BLUE, LuxuryTheme.ACCENT_TEAL, LuxuryTheme.ACCENT_VIOLET};
            for (int i = 0; i < swatchColors.length; i++) {
                Color col = swatchColors[i];
                String name = swatchNames[i];
                JButton swatch = new JButton(name) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(col);
                        g2.fill(new java.awt.geom.RoundRectangle2D.Double(0,0,getWidth(),getHeight(),10,10));
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                swatch.setFont(LuxuryTheme.FONT_BADGE);
                swatch.setForeground(Color.BLACK);
                swatch.setContentAreaFilled(false);
                swatch.setBorderPainted(false);
                swatch.setFocusPainted(false);
                swatch.setPreferredSize(new Dimension(70, 36));
                swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                swatchRow.add(swatch);
            }
            card.add(swatchRow);
            card.add(Box.createVerticalStrut(24));

            JSeparator sep = LuxuryTheme.separator();
            sep.setAlignmentX(LEFT_ALIGNMENT);
            card.add(sep);
            card.add(Box.createVerticalStrut(16));

            // Font size
            JLabel fontTitle = LuxuryTheme.mutedLabel("Font Scale");
            fontTitle.setAlignmentX(LEFT_ALIGNMENT);
            card.add(fontTitle);
            card.add(Box.createVerticalStrut(8));
            JPanel fontRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            fontRow.setOpaque(false);
            ButtonGroup fontGroup = new ButtonGroup();
            for (String sz : new String[]{"Small", "Default", "Large", "X-Large"}) {
                JRadioButton rb = new JRadioButton(sz);
                rb.setOpaque(false); rb.setForeground(LuxuryTheme.TEXT_PRIMARY);
                if ("Default".equals(sz)) rb.setSelected(true);
                fontGroup.add(rb); fontRow.add(rb);
            }
            card.add(fontRow);
            card.add(Box.createVerticalStrut(20));

            JButton applyBtn = LuxuryTheme.goldButton("  Apply Settings  ");
            applyBtn.setAlignmentX(LEFT_ALIGNMENT);
            applyBtn.addActionListener(e -> AppUtils.showSuccess(card, "Theme preferences saved. Restart may be required."));
            card.add(applyBtn);

            add(card);
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // System Configuration (key/value store from ui_system_config)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class SystemConfigPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;

        SystemConfigPanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(LuxuryTheme.BG_PANEL);

            JPanel top = new JPanel(new BorderLayout(16, 0));
            top.setBackground(LuxuryTheme.BG_PANEL);
            top.setBorder(BorderFactory.createEmptyBorder(16, 28, 0, 28));

            JLabel hint = new JLabel("  🔑  Key-value system settings stored in ui_system_config.");
            hint.setFont(LuxuryTheme.FONT_SMALL);
            hint.setForeground(LuxuryTheme.TEXT_MUTED);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnRow.setOpaque(false);
            JButton addBtn = LuxuryTheme.goldButton("+ Add Config");
            JButton editBtn = LuxuryTheme.primaryButton("✎ Edit");
            JButton refreshBtn = LuxuryTheme.ghostButton("↻");
            addBtn.addActionListener(e -> openAddConfigDialog());
            editBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) openEditConfigDialog(row);
                else AppUtils.showError(this, "Select a row to edit.");
            });
            refreshBtn.addActionListener(e -> loadConfigs());
            btnRow.add(refreshBtn); btnRow.add(addBtn); btnRow.add(editBtn);

            top.add(hint, BorderLayout.WEST);
            top.add(btnRow, BorderLayout.EAST);

            model = AppUtils.tableModel(new String[]{"Config Key","Config Value","Description","Last Modified"});
            table = new JTable(model);
            JScrollPane sp = LuxuryTheme.styledTable(table);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());

            JPanel tableWrap = new JPanel(new BorderLayout());
            tableWrap.setBackground(LuxuryTheme.BG_PANEL);
            tableWrap.setBorder(BorderFactory.createEmptyBorder(10, 28, 16, 28));
            tableWrap.add(sp, BorderLayout.CENTER);

            // Pre-populate with static defaults (ui_system_config may be empty on fresh install)
            add(top, BorderLayout.NORTH);
            add(tableWrap, BorderLayout.CENTER);

            loadConfigs();
        }

        private void loadConfigs() {
            // In a full implementation, SystemConfigDAO would load from ui_system_config.
            // Here we display hardcoded sensible defaults as demo rows.
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                String[][] defaults = {
                    {"app.version",       "1.0.0",  "Application version",               AppUtils.today()},
                    {"db.pool.size",      "10",     "JDBC connection pool size",          AppUtils.today()},
                    {"session.timeout",   "3600",   "Session timeout in seconds",         AppUtils.today()},
                    {"max.login.attempts","5",      "Max failed login attempts before lock", AppUtils.today()},
                    {"default.currency",  "INR",    "Default currency code",              AppUtils.today()},
                    {"audit.enabled",     "true",   "Enable audit trail logging",         AppUtils.today()},
                    {"reorder.auto",      "false",  "Auto-create reorder POs",            AppUtils.today()},
                    {"barcode.mode",      "SCAN",   "Default barcode event type",         AppUtils.today()},
                };
                for (String[] row : defaults) model.addRow(row);
            });
        }

        private void openAddConfigDialog() {
            JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Config Key", true);
            d.setSize(460, 280); d.setLocationRelativeTo(this);
            d.getContentPane().setBackground(LuxuryTheme.BG_CARD);
            d.setLayout(new BorderLayout());
            JTextField keyF   = LuxuryTheme.textField("config.key");
            JTextField valF   = LuxuryTheme.textField("value");
            JTextField descF  = LuxuryTheme.textField("Description");
            JPanel form = new CrudPanel.FormBuilder().addField("Key *", keyF).addField("Value *", valF).addField("Description", descF).build();
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            btnPanel.setBackground(LuxuryTheme.BG_CARD);
            JButton cancel = LuxuryTheme.ghostButton("Cancel"); cancel.addActionListener(ev -> d.dispose());
            JButton save = LuxuryTheme.goldButton("Save"); save.addActionListener(ev -> {
                model.addRow(new Object[]{keyF.getText().trim(), valF.getText().trim(), descF.getText().trim(), AppUtils.today()});
                d.dispose();
            });
            btnPanel.add(cancel); btnPanel.add(save);
            d.add(form, BorderLayout.CENTER); d.add(btnPanel, BorderLayout.SOUTH);
            d.setVisible(true);
        }

        private void openEditConfigDialog(int row) {
            String currentVal = (String) model.getValueAt(row, 1);
            String key = (String) model.getValueAt(row, 0);
            JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Config: " + key, true);
            d.setSize(420, 220); d.setLocationRelativeTo(this);
            d.getContentPane().setBackground(LuxuryTheme.BG_CARD);
            d.setLayout(new BorderLayout());
            JTextField valF = LuxuryTheme.textField(currentVal);
            JPanel form = new CrudPanel.FormBuilder().addField("Value", valF).build();
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            btnPanel.setBackground(LuxuryTheme.BG_CARD);
            JButton cancel = LuxuryTheme.ghostButton("Cancel"); cancel.addActionListener(ev -> d.dispose());
            JButton save = LuxuryTheme.goldButton("Save"); save.addActionListener(ev -> {
                model.setValueAt(valF.getText().trim(), row, 1);
                model.setValueAt(AppUtils.today(), row, 3);
                d.dispose();
            });
            btnPanel.add(cancel); btnPanel.add(save);
            d.add(form, BorderLayout.CENTER); d.add(btnPanel, BorderLayout.SOUTH);
            d.setVisible(true);
        }
    }
}
