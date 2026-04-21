package scm.ui;

import scm.ui.auth.LoginPanel;
import scm.ui.dashboard.DashboardPanel;
import scm.ui.delivery.DeliveryMonitoringPanel;
import scm.ui.forecast.ForecastPanel;
import scm.ui.inventory.InventoryPanel;
import scm.ui.logistics.LogisticsPanel;
import scm.ui.model.UIUser;
import scm.ui.navigation.NavigationPanel;
import scm.ui.notifications.NotificationPanel;
import scm.ui.orders.OrderPanel;
import scm.ui.patterns.EventBus;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.pricing.PricingPanel;
import scm.ui.settings.SettingsPanel;
import scm.ui.util.LuxuryTheme;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * MainFrame — The primary JFrame for the SCM Nexus UI Subsystem.
 *
 * Lifecycle:
 *   1. Show LoginPanel (full-screen)
 *   2. On successful login → build role-filtered NavigationPanel + content CardLayout
 *   3. Navigation events switch the active content panel
 *   4. Logout clears session and returns to LoginPanel
 */
public class MainFrame extends JFrame {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    private JPanel rootPanel;       // CardLayout root: LOGIN | APP
    private JPanel appShell;        // BorderLayout: sidebar + header + content
    private CardLayout contentLayout;
    private JPanel contentArea;
    private NavigationPanel navPanel;
    private JLabel breadcrumbLabel;
    private JLabel userInfoLabel;
    private JLabel notifCountLabel;

    // Content panels (lazily created after login so we have the user context)
    private DashboardPanel   dashboardPanel;
    private InventoryPanel   inventoryPanel;
    private OrderPanel       orderPanel;
    private LogisticsPanel   logisticsPanel;
    private PricingPanel     pricingPanel;
    private ForecastPanel    forecastPanel;
    private NotificationPanel notificationPanel;
    private SettingsPanel    settingsPanel;
    private DeliveryMonitoringPanel deliveryMonitoringPanel;

    public MainFrame() {
        super("SCM Nexus — Supply Chain Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1200, 750));
        setLocationRelativeTo(null);
        setBackground(LuxuryTheme.BG_DEEP);

        LuxuryTheme.apply();
        buildRootLayout();
        subscribeEvents();
    }

    // ── Root layout (LOGIN / APP cards) ──────────────────────────────────────
    private void buildRootLayout() {
        rootPanel = new JPanel(new CardLayout());
        rootPanel.setBackground(LuxuryTheme.BG_DEEP);

        LoginPanel loginPanel = new LoginPanel(this::onLoginSuccess);
        rootPanel.add(loginPanel, "LOGIN");

        setContentPane(rootPanel);
    }

    // ── Called after successful authentication ────────────────────────────────
    private void onLoginSuccess(UIUser user) {
        buildAppShell(user);
        switchToPanel("DASHBOARD");

        CardLayout cl = (CardLayout) rootPanel.getLayout();
        cl.show(rootPanel, "APP");
    }

    private void buildAppShell(UIUser user) {
        appShell = new JPanel(new BorderLayout());
        appShell.setBackground(LuxuryTheme.BG_PANEL);

        // ── Sidebar ───────────────────────────────────────────────────────
        navPanel = new NavigationPanel(user, panelId -> switchToPanel(panelId));
        breadcrumbLabel = new JLabel("Home  ›  Dashboard");
        breadcrumbLabel.setFont(LuxuryTheme.FONT_SMALL);
        breadcrumbLabel.setForeground(LuxuryTheme.TEXT_MUTED);
        navPanel.setBreadcrumbLabel(breadcrumbLabel);

        // ── Top header bar ────────────────────────────────────────────────
        JPanel topBar = buildTopBar(user);

        // ── Content area (CardLayout) ─────────────────────────────────────
        contentLayout = new CardLayout();
        contentArea   = new JPanel(contentLayout);
        contentArea.setBackground(LuxuryTheme.BG_PANEL);

        // Create all panels
        dashboardPanel     = new DashboardPanel();
        inventoryPanel     = new InventoryPanel();
        orderPanel         = new OrderPanel();
        logisticsPanel     = new LogisticsPanel();
        pricingPanel       = new PricingPanel();
        forecastPanel      = new ForecastPanel();
        notificationPanel  = new NotificationPanel();
        settingsPanel      = new SettingsPanel();
        deliveryMonitoringPanel = new DeliveryMonitoringPanel();

        // Wire notification badge callback
        notificationPanel.setBadgeCallback(count -> {
            navPanel.updateNotificationBadge(count);
            if (notifCountLabel != null) {
                notifCountLabel.setText(count > 0 ? " " + count + " " : "");
                notifCountLabel.setVisible(count > 0);
            }
        });

        contentArea.add(dashboardPanel,    "DASHBOARD");
        contentArea.add(inventoryPanel,    "INVENTORY");
        contentArea.add(orderPanel,        "ORDERS");
        contentArea.add(logisticsPanel,    "LOGISTICS");
        contentArea.add(pricingPanel,      "PRICING");
        contentArea.add(forecastPanel,     "FORECASTING");
        contentArea.add(notificationPanel, "NOTIFICATIONS");
        contentArea.add(settingsPanel,     "SETTINGS");
        contentArea.add(deliveryMonitoringPanel, "DELIVERY_MONITORING");

        // ── Assemble ──────────────────────────────────────────────────────
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(LuxuryTheme.BG_PANEL);
        mainArea.add(topBar,     BorderLayout.NORTH);
        mainArea.add(contentArea, BorderLayout.CENTER);

        appShell.add(navPanel, BorderLayout.WEST);
        appShell.add(mainArea, BorderLayout.CENTER);

        rootPanel.add(appShell, "APP");
    }

    private JPanel buildTopBar(UIUser user) {
        JPanel topBar = new JPanel(new BorderLayout(20, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(LuxuryTheme.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(LuxuryTheme.BORDER_SUBTLE);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        topBar.setPreferredSize(new Dimension(0, 54));

        // Breadcrumb
        JPanel breadcrumbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        breadcrumbRow.setOpaque(false);
        breadcrumbRow.add(breadcrumbLabel);

        // Right side: notifications bell + user label
        JPanel rightRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightRow.setOpaque(false);

        // Notification bell
        JButton bellBtn = new JButton("🔔") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(LuxuryTheme.BG_HOVER);
                    g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),8,8));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bellBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        bellBtn.setForeground(LuxuryTheme.TEXT_SECOND);
        bellBtn.setContentAreaFilled(false);
        bellBtn.setBorderPainted(false);
        bellBtn.setFocusPainted(false);
        bellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellBtn.setPreferredSize(new Dimension(40, 34));
        bellBtn.addActionListener(e -> switchToPanel("NOTIFICATIONS"));

        notifCountLabel = new JLabel("") {
            @Override protected void paintComponent(Graphics g) {
                if (!getText().isBlank()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(LuxuryTheme.DANGER);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        notifCountLabel.setFont(LuxuryTheme.FONT_BADGE);
        notifCountLabel.setForeground(Color.WHITE);
        notifCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        notifCountLabel.setPreferredSize(new Dimension(20, 20));
        notifCountLabel.setVisible(false);

        // User info
        userInfoLabel = new JLabel(user.getDisplayName() + "  ·  " + user.getUserRole());
        userInfoLabel.setFont(LuxuryTheme.FONT_SMALL);
        userInfoLabel.setForeground(LuxuryTheme.TEXT_SECOND);

        // Separator
        JSeparator vs = new JSeparator(JSeparator.VERTICAL);
        vs.setPreferredSize(new Dimension(1, 22));
        vs.setForeground(LuxuryTheme.BORDER_SUBTLE);

        rightRow.add(userInfoLabel);
        rightRow.add(vs);
        rightRow.add(bellBtn);
        rightRow.add(notifCountLabel);

        topBar.add(breadcrumbRow, BorderLayout.WEST);
        topBar.add(rightRow, BorderLayout.EAST);

        return topBar;
    }

    // ── Panel switching ───────────────────────────────────────────────────────
    private void switchToPanel(String panelId) {
        if (contentLayout == null || contentArea == null) return;
        contentLayout.show(contentArea, panelId);
        navPanel.setActive(panelId);

        // Trigger data refresh on the visible panel
        SwingUtilities.invokeLater(() -> {
            switch (panelId) {
                case "DASHBOARD":            dashboardPanel.loadData(); break;
                case "NOTIFICATIONS":        break; // self-refreshes via EventBus
                case "DELIVERY_MONITORING":  break; // auto-refreshes via scheduler + events
                default:                     break;
            }
        });
    }

    // ── EventBus subscriptions ────────────────────────────────────────────────
    private void subscribeEvents() {
        EventBus.getInstance().subscribe(EventBus.Event.USER_LOGGED_OUT, payload -> {
            SwingUtilities.invokeLater(() -> {
                // Destroy app shell and return to login
                if (rootPanel.getComponentCount() > 1) rootPanel.remove(appShell);
                appShell = null;
                CardLayout cl = (CardLayout) rootPanel.getLayout();
                cl.show(rootPanel, "LOGIN");
                navPanel = null;
                dashboardPanel = null; inventoryPanel = null; orderPanel = null;
                logisticsPanel = null; pricingPanel = null; forecastPanel = null;
                if (deliveryMonitoringPanel != null) {
                    deliveryMonitoringPanel.shutdown();
                    deliveryMonitoringPanel = null;
                }
                notificationPanel = null; settingsPanel = null;
                repaint();
            });
        });
    }
}
