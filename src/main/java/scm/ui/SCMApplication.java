package scm.ui;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.db.DatabaseModuleBootstrap;
import scm.ui.model.UIUser;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.AppUtils;
import scm.ui.util.LuxuryTheme;
import javax.swing.*;

/**
 * SCMApplication — Entry point for the SCM Nexus UI Subsystem (Feature #16).
 *
 * On startup:
 *  1. Applies the LuxuryTheme global Look & Feel settings
 *  2. Verifies the database connection (gracefully falls back to DEMO mode)
 *  3. Seeds a default ADMIN user if the user table is empty
 *  4. Initialises integrated subsystem bridges (including exception subsystem)
 *  5. Launches MainFrame on the Event Dispatch Thread
 *
 * Design Patterns in use across the subsystem:
 *  Creational:  Singleton (DatabaseConnectionPool), Factory (DAOFactory), Builder (CrudPanel.FormBuilder)
 *  Structural:  Facade (SupplyChainFacade), Adapter (BarcodeReaderAdapter), Template Method (BaseDAO, CrudPanel)
 *  Behavioural: Observer (EventBus), Strategy (AppUtils.ExportStrategy)
 *
 * Exception integration is now self-routed through scm-exception-handler-v3.jar.
 * UI and Inventory exception sources call subsystem APIs directly; no manual
 * handler wiring is required in SCMApplication.
 */
public class SCMApplication {

    public static void main(String[] args) {
        // ── 1. Apply Luxury theme (must be on EDT) ────────────────────────
        SwingUtilities.invokeLater(() -> {
            LuxuryTheme.apply();

            // ── 2. Splash screen ──────────────────────────────────────────
            JWindow splash = buildSplash();
            splash.setVisible(true);

            // ── 3. Background init ────────────────────────────────────────
            SwingWorker<Boolean, String> initWorker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    publish("Bootstrapping database module…");
                    boolean dbOk = false;
                    try {
                        DatabaseModuleBootstrap.bootstrap();
                        DatabaseConnectionPool pool = DatabaseConnectionPool.getInstance();
                        java.sql.Connection c = pool.getConnection();
                        if (c != null) { pool.releaseConnection(c); dbOk = true; }
                    } catch (Exception e) {
                        System.out.println("[SCM] Database unavailable — running in DEMO mode.");
                    }

                    publish("Seeding demo data…");
                    if (dbOk) seedDemoUsers();

                    publish("Loading UI…");
                    Thread.sleep(600); // small delay so splash is visible
                    return dbOk;
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    // Could update splash label here
                }

                @Override
                protected void done() {
                    splash.dispose();

                    System.out.println("[SCM] Exception subsystem ready (v3 self-routed).");
                    // ── Register handler: Delivery Monitoring Subsystem ────────────
                    // DeliveryMonitoringFacadeDB uses SCMExceptionHandler.INSTANCE
                    // (real exception module) internally — it is self-registering.
                    // We only verify the delivery system is available here.
                    if (SupplyChainFacade.getInstance().getDeliverySystem() != null) {
                        System.out.println("[SCM] Real-Time Delivery Monitoring subsystem ready.");
                    } else {
                        System.out.println("[SCM] WARNING: Delivery Monitoring JAR not found in lib/ — panel will show error.");
                    }

                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                }
            };
            initWorker.execute();
        });
    }

    // ── Seed demo admin / staff users ────────────────────────────────────────
    private static void seedDemoUsers() {
        SupplyChainFacade facade = SupplyChainFacade.getInstance();

        // Only seed if no users exist
        if (!facade.getAllUsers().isEmpty()) return;

        String[][] users = {
            {"admin",     "Administrator",   "admin@scm.local",    "ADMIN",          "admin123"},
            {"manager",   "Priya Sharma",    "priya@scm.local",    "MANAGER",        "manager123"},
            {"warehouse", "Ravi Kumar",      "ravi@scm.local",     "WAREHOUSE_STAFF","staff123"},
            {"sales",     "Anjali Patel",    "anjali@scm.local",   "SALES_REP",      "sales123"},
            {"analyst",   "Deepak Menon",    "deepak@scm.local",   "ANALYST",        "analyst123"},
            {"driver",    "Suresh Yadav",    "suresh@scm.local",   "LOGISTICS_STAFF","driver123"}
        };

        for (String[] u : users) {
            UIUser user = new UIUser();
            user.setUserId(AppUtils.newId("USR").hashCode());
            user.setUsername(u[0]);
            user.setDisplayName(u[1]);
            user.setEmail(u[2]);
            user.setUserRole(u[3]);
            user.setPasswordHash(AppUtils.hashPassword(u[4]));
            user.setStatus("ACTIVE");
            user.setAccountLocked(false);
            try { facade.createUser(user); } catch (Exception ignored) {}
        }
        System.out.println("[SCM] Demo users seeded.");
    }

    // ── Splash screen ─────────────────────────────────────────────────────────
    private static JWindow buildSplash() {
        JWindow splash = new JWindow();
        splash.setSize(480, 270);
        splash.setLocationRelativeTo(null);

        JPanel content = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                // Background gradient
                g2.setPaint(new java.awt.GradientPaint(0, 0, LuxuryTheme.BG_DEEP,
                                                        0, getHeight(), LuxuryTheme.BG_PANEL));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Border
                g2.setColor(LuxuryTheme.ACCENT_GOLD);
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawRoundRect(2, 2, getWidth()-4, getHeight()-4, 16, 16);
                // Logo mark
                g2.setFont(new java.awt.Font("Segoe UI Symbol", java.awt.Font.PLAIN, 52));
                g2.setColor(LuxuryTheme.ACCENT_GOLD);
                g2.drawString("◈", 30, 90);
                // Title
                g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));
                g2.setColor(LuxuryTheme.TEXT_GOLD);
                g2.drawString("SCM NEXUS", 100, 80);
                // Subtitle
                g2.setFont(LuxuryTheme.FONT_BODY);
                g2.setColor(LuxuryTheme.TEXT_SECOND);
                g2.drawString("Supply Chain Management", 100, 108);
                // Feature tag
                g2.setFont(LuxuryTheme.FONT_SMALL);
                g2.setColor(LuxuryTheme.TEXT_MUTED);
                g2.drawString("Feature #16  ·  UI Subsystem v1.0  ·  Java Swing", 30, 160);
                g2.drawString("Database: MySQL OOAD  ·  10 UI Components", 30, 180);
                // Loading bar
                g2.setColor(LuxuryTheme.BG_CARD);
                g2.fillRoundRect(30, 220, 420, 8, 8, 8);
                g2.setColor(LuxuryTheme.ACCENT_GOLD);
                g2.fillRoundRect(30, 220, 280, 8, 8, 8);
                g2.setColor(LuxuryTheme.TEXT_MUTED);
                g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 10));
                g2.drawString("Initialising…", 30, 248);
                g2.dispose();
            }
        };
        content.setBackground(LuxuryTheme.BG_DEEP);
        splash.setContentPane(content);
        return splash;
    }
}
