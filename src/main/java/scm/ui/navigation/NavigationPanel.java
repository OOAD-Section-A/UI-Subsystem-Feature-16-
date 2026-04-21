package scm.ui.navigation;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import scm.ui.model.UIUser;
import scm.ui.patterns.EventBus;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.LuxuryTheme;

/**
 * C-02 — Navigation & Layout Controller
 * Renders the application sidebar, top header, breadcrumb,
 * and routes panels based on user role.
 *
 * STRUCTURAL PATTERN — Composite: menu items form a tree.
 * BEHAVIOURAL PATTERN — Observer: listens to EventBus for badge updates.
 */
public class NavigationPanel extends JPanel {

    public interface PanelSwitchListener {
        void onPanelSwitch(String panelId);
    }

    // ── Menu item descriptor ─────────────────────────────────────────────────
    public static class MenuItem {
        public final String id, label, icon;
        public final String[] allowedRoles;
        public MenuItem(String id, String label, String icon, String... roles) {
            this.id = id; this.label = label; this.icon = icon; this.allowedRoles = roles;
        }
    }

    // ── Full menu definition (role-filtered per C-02) ────────────────────────
    private static final MenuItem[] ALL_MENU = {
        new MenuItem("DASHBOARD",    "Dashboard",         "▣",  "ADMIN","MANAGER","ANALYST"),
        new MenuItem("INVENTORY",    "Inventory",         "⊞",  "ADMIN","MANAGER","WAREHOUSE_STAFF"),
        new MenuItem("ORDERS",       "Orders",            "📋", "ADMIN","MANAGER","SALES_REP","CASHIER","WAREHOUSE_STAFF"),
        new MenuItem("LOGISTICS",    "Transport & Logistics","🚚","ADMIN","MANAGER","DRIVER"),
        new MenuItem("PRICING",      "Pricing & Discounts","💰","ADMIN","MANAGER"),
        new MenuItem("FORECASTING",  "Demand Forecasting","📈", "ADMIN","MANAGER","ANALYST"),
        new MenuItem("NOTIFICATIONS","Notifications",     "🔔", "ADMIN","MANAGER","WAREHOUSE_STAFF","SALES_REP","CASHIER","ANALYST","DRIVER"),
        new MenuItem("SETTINGS",          "Settings",             "⚙",  "ADMIN"),
        new MenuItem("DELIVERY_MONITORING","Delivery Monitoring", "📍", "ADMIN","MANAGER","DRIVER"),
    };

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();
    private final UIUser user;
    private final PanelSwitchListener switchListener;

    private String activePanel = "DASHBOARD";
    private final Map<String, JButton> menuButtons = new LinkedHashMap<>();
    private JLabel notifBadge;
    private JLabel breadcrumb;

    public NavigationPanel(UIUser user, PanelSwitchListener switchListener) {
        this.user = user;
        this.switchListener = switchListener;
        buildSidebar();
        subscribeEvents();
    }

    private void buildSidebar() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_SIDEBAR);
        setPreferredSize(new Dimension(220, 0));

        // ── Brand ─────────────────────────────────────────────────────────
        JPanel brand = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(LuxuryTheme.BG_DEEP);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(LuxuryTheme.ACCENT_GOLD);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(16, getHeight()-1, getWidth()-16, getHeight()-1);
                g2.dispose();
            }
        };
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(22, 20, 18, 20));

        JLabel logoMark = new JLabel("◈  SCM NEXUS");
        logoMark.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoMark.setForeground(LuxuryTheme.TEXT_GOLD);
        logoMark.setAlignmentX(LEFT_ALIGNMENT);

        JLabel roleLabel = new JLabel(user.getUserRole().replace('_', ' '));
        roleLabel.setFont(LuxuryTheme.FONT_SMALL);
        roleLabel.setForeground(LuxuryTheme.TEXT_MUTED);
        roleLabel.setAlignmentX(LEFT_ALIGNMENT);

        brand.add(logoMark);
        brand.add(Box.createVerticalStrut(4));
        brand.add(roleLabel);

        // ── User chip ─────────────────────────────────────────────────────
        JPanel userChip = new JPanel(new BorderLayout(10, 0));
        userChip.setOpaque(false);
        userChip.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel avatar = new JLabel(String.valueOf(user.getDisplayName().charAt(0)).toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LuxuryTheme.ACCENT_GOLD);
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.drawString(getText(), 11, 24);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);
        JLabel nameLabel = new JLabel(user.getDisplayName());
        nameLabel.setFont(LuxuryTheme.FONT_SUBHEAD);
        nameLabel.setForeground(LuxuryTheme.TEXT_PRIMARY);
        JLabel emailLabel = new JLabel(truncate(user.getEmail(), 22));
        emailLabel.setFont(LuxuryTheme.FONT_SMALL);
        emailLabel.setForeground(LuxuryTheme.TEXT_MUTED);
        namePanel.add(nameLabel);
        namePanel.add(emailLabel);

        userChip.add(avatar, BorderLayout.WEST);
        userChip.add(namePanel, BorderLayout.CENTER);

        // ── Menu items ────────────────────────────────────────────────────
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        for (MenuItem item : ALL_MENU) {
            if (!hasAccess(item)) continue;
            JButton btn = buildMenuButton(item);
            menuButtons.put(item.id, btn);
            menuPanel.add(btn);
            menuPanel.add(Box.createVerticalStrut(2));
        }

        // ── Notification badge on button ──────────────────────────────────
        JButton notifBtn = menuButtons.get("NOTIFICATIONS");
        if (notifBtn != null) {
            notifBadge = new JLabel("0");
            notifBadge.setFont(LuxuryTheme.FONT_BADGE);
            notifBadge.setForeground(Color.WHITE);
            notifBadge.setOpaque(false);
        }

        // ── Logout at bottom ──────────────────────────────────────────────
        JPanel logoutPanel = new JPanel(new BorderLayout());
        logoutPanel.setOpaque(false);
        logoutPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 16, 10));
        JButton logoutBtn = buildIconButton("⏻  Sign Out", LuxuryTheme.DANGER);
        logoutBtn.addActionListener(e -> {
            if (AppUtils.confirm(this, "Sign out?")) {
                facade.logout();
                EventBus.getInstance().publish(EventBus.Event.USER_LOGGED_OUT);
            }
        });
        logoutPanel.add(logoutBtn);

        // ── Scroll menu ───────────────────────────────────────────────────
        JScrollPane menuScroll = new JScrollPane(menuPanel);
        menuScroll.setOpaque(false);
        menuScroll.getViewport().setOpaque(false);
        menuScroll.setBorder(BorderFactory.createEmptyBorder());
        menuScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(brand, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(userChip, BorderLayout.NORTH);
        centerPanel.add(menuScroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        add(logoutPanel, BorderLayout.SOUTH);

        // Set initial active
        setActive("DASHBOARD");
    }

    private JButton buildMenuButton(MenuItem item) {
        JButton btn = new JButton(item.icon + "  " + item.label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = item.id.equals(activePanel);
                if (active) {
                    g2.setColor(LuxuryTheme.BG_SELECTED);
                    g2.fill(new RoundRectangle2D.Double(4, 2, getWidth()-8, getHeight()-4, 10, 10));
                    g2.setColor(LuxuryTheme.ACCENT_GOLD);
                    g2.fillRoundRect(0, 8, 4, getHeight()-16, 4, 4);
                } else if (getModel().isRollover()) {
                    g2.setColor(LuxuryTheme.BG_HOVER);
                    g2.fill(new RoundRectangle2D.Double(4, 2, getWidth()-8, getHeight()-4, 10, 10));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(item.id.equals(activePanel) ? LuxuryTheme.FONT_NAV_SEL : LuxuryTheme.FONT_NAV);
        btn.setForeground(item.id.equals(activePanel) ? LuxuryTheme.TEXT_PRIMARY : LuxuryTheme.TEXT_SECOND);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 10));
        btn.addActionListener(e -> {
            setActive(item.id);
            if (switchListener != null) switchListener.onPanelSwitch(item.id);
        });
        return btn;
    }

    private JButton buildIconButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(LuxuryTheme.FONT_BODY);
        btn.setForeground(color);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return btn;
    }

    public void setActive(String panelId) {
        activePanel = panelId;
        menuButtons.forEach((id, btn) -> {
            boolean active = id.equals(panelId);
            btn.setFont(active ? LuxuryTheme.FONT_NAV_SEL : LuxuryTheme.FONT_NAV);
            btn.setForeground(active ? LuxuryTheme.TEXT_PRIMARY : LuxuryTheme.TEXT_SECOND);
            btn.repaint();
        });
        updateBreadcrumb();
        // Persist panel state
        facade.persistPanelState(panelId);
    }

    public void updateNotificationBadge(int count) {
        if (notifBadge != null) {
            notifBadge.setText(count > 0 ? String.valueOf(count) : "");
            notifBadge.setVisible(count > 0);
        }
        JButton notifBtn = menuButtons.get("NOTIFICATIONS");
        if (notifBtn != null) {
            String base = "🔔  Notifications";
            notifBtn.setText(count > 0 ? base + " (" + count + ")" : base);
        }
    }

    private void updateBreadcrumb() {
        if (breadcrumb == null) return;
        String label = "Home";
        for (MenuItem m : ALL_MENU) {
            if (m.id.equals(activePanel)) { label = m.label; break; }
        }
        breadcrumb.setText("Home  ›  " + label);
    }

    public void setBreadcrumbLabel(JLabel lbl) {
        this.breadcrumb = lbl;
        updateBreadcrumb();
    }

    private void subscribeEvents() {
        EventBus.getInstance().subscribe(EventBus.Event.NOTIFICATION_RECEIVED, payload -> {
            SwingUtilities.invokeLater(() -> updateNotificationBadge(facade.getUnreadNotificationCount()));
        });
        EventBus.getInstance().subscribe(EventBus.Event.NOTIFICATION_READ, payload -> {
            SwingUtilities.invokeLater(() -> updateNotificationBadge(facade.getUnreadNotificationCount()));
        });
    }

    private boolean hasAccess(MenuItem item) {
        if (item.allowedRoles == null || item.allowedRoles.length == 0) return true;
        for (String role : item.allowedRoles) {
            if (role.equalsIgnoreCase(user.getUserRole())) return true;
        }
        return false;
    }

    private String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) + "…" : s;
    }

    // Keep AppUtils accessible here (avoid import issue)
    private static class AppUtils {
        static boolean confirm(Component parent, String msg) {
            return JOptionPane.showConfirmDialog(parent, msg, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }
    }
}
