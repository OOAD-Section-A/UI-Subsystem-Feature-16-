package scm.ui.auth;

import scm.ui.exceptions.UIAuthExceptionSource;
import scm.ui.model.UIUser;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.AppUtils;
import scm.ui.util.LuxuryTheme;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * C-01 — Authentication & RBAC Module
 * Login screen with username/password, session management, and role-based
 * redirect. Uses bcrypt-like hash comparison (SHA-256 for demo).
 *
 * CREATIONAL — Builder pattern for building the login form layout.
 */
public class LoginPanel extends JPanel {

    public interface LoginListener {
        void onLoginSuccess(UIUser user);
    }

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();
    private final LoginListener listener;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private JButton loginButton;
    private int failCount = 0;

    public LoginPanel(LoginListener listener) {
        this.listener = listener;
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_DEEP);

        // ── Left decorative panel ─────────────────────────────────────────
        JPanel leftPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // gradient background
                GradientPaint gp = new GradientPaint(0, 0, new Color(10,12,30),
                    getWidth(), getHeight(), new Color(20,28,60));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // decorative circles
                g2.setColor(new Color(212, 175, 55, 15));
                g2.fillOval(-80, -80, 400, 400);
                g2.setColor(new Color(64, 156, 255, 10));
                g2.fillOval(100, 200, 300, 300);
                g2.dispose();
            }
        };
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setPreferredSize(new Dimension(400, 0));

        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);

        // Logo/icon area
        JLabel logoIcon = new JLabel("◈") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LuxuryTheme.ACCENT_GOLD);
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 80));
                g2.drawString("◈", 0, 80);
                g2.dispose();
            }
        };
        logoIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 80));
        logoIcon.setForeground(LuxuryTheme.ACCENT_GOLD);
        logoIcon.setAlignmentX(CENTER_ALIGNMENT);
        logoIcon.setPreferredSize(new Dimension(100, 90));

        JLabel brandTitle = new JLabel("SCM NEXUS");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 30));
        brandTitle.setForeground(LuxuryTheme.TEXT_GOLD);
        brandTitle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel brandSub = new JLabel("Supply Chain Management");
        brandSub.setFont(LuxuryTheme.FONT_BODY);
        brandSub.setForeground(LuxuryTheme.TEXT_SECOND);
        brandSub.setAlignmentX(CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(212, 175, 55, 60));
        sep.setMaximumSize(new Dimension(200, 1));

        JLabel ver = new JLabel("Feature #16 — UI Subsystem v1.0");
        ver.setFont(LuxuryTheme.FONT_SMALL);
        ver.setForeground(LuxuryTheme.TEXT_MUTED);
        ver.setAlignmentX(CENTER_ALIGNMENT);

        brandPanel.add(logoIcon);
        brandPanel.add(Box.createVerticalStrut(10));
        brandPanel.add(brandTitle);
        brandPanel.add(Box.createVerticalStrut(6));
        brandPanel.add(brandSub);
        brandPanel.add(Box.createVerticalStrut(20));
        brandPanel.add(sep);
        brandPanel.add(Box.createVerticalStrut(20));
        brandPanel.add(ver);

        leftPanel.add(brandPanel);

        // ── Right login form ──────────────────────────────────────────────
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(LuxuryTheme.BG_PANEL);

        JPanel formCard = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LuxuryTheme.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),20,20));
                g2.setColor(new Color(212,175,55,40));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1,1,getWidth()-2,getHeight()-2,20,20));
                g2.dispose();
            }
        };
        formCard.setOpaque(false);
        formCard.setLayout(new GridBagLayout());
        formCard.setPreferredSize(new Dimension(420, 520));
        formCard.setBorder(BorderFactory.createEmptyBorder(40,40,40,40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        JLabel signInLabel = new JLabel("Welcome Back");
        signInLabel.setFont(LuxuryTheme.FONT_TITLE);
        signInLabel.setForeground(LuxuryTheme.TEXT_PRIMARY);
        signInLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel signInSub = new JLabel("Sign in to your account");
        signInSub.setFont(LuxuryTheme.FONT_BODY);
        signInSub.setForeground(LuxuryTheme.TEXT_SECOND);
        signInSub.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel userLabel = new JLabel("USERNAME");
        userLabel.setFont(LuxuryTheme.FONT_BADGE);
        userLabel.setForeground(LuxuryTheme.TEXT_SECOND);

        usernameField = LuxuryTheme.textField("");
        usernameField.setPreferredSize(new Dimension(340, 44));

        JLabel passLabel = new JLabel("PASSWORD");
        passLabel.setFont(LuxuryTheme.FONT_BADGE);
        passLabel.setForeground(LuxuryTheme.TEXT_SECOND);

        passwordField = LuxuryTheme.passwordField();
        passwordField.setPreferredSize(new Dimension(340, 44));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(LuxuryTheme.FONT_SMALL);
        statusLabel.setForeground(LuxuryTheme.DANGER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        loginButton = LuxuryTheme.goldButton("  SIGN IN  ");
        loginButton.setPreferredSize(new Dimension(340, 46));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel demoHint = new JLabel("Demo: admin / admin123");
        demoHint.setFont(LuxuryTheme.FONT_SMALL);
        demoHint.setForeground(LuxuryTheme.TEXT_MUTED);
        demoHint.setHorizontalAlignment(SwingConstants.CENTER);

        formCard.add(signInLabel, gbc);
        formCard.add(Box.createVerticalStrut(4), gbc);
        formCard.add(signInSub, gbc);
        formCard.add(Box.createVerticalStrut(24), gbc);
        formCard.add(userLabel, gbc);
        formCard.add(Box.createVerticalStrut(4), gbc);
        formCard.add(usernameField, gbc);
        formCard.add(Box.createVerticalStrut(16), gbc);
        formCard.add(passLabel, gbc);
        formCard.add(Box.createVerticalStrut(4), gbc);
        formCard.add(passwordField, gbc);
        formCard.add(Box.createVerticalStrut(10), gbc);
        formCard.add(statusLabel, gbc);
        formCard.add(Box.createVerticalStrut(10), gbc);
        formCard.add(loginButton, gbc);
        formCard.add(Box.createVerticalStrut(16), gbc);
        formCard.add(demoHint, gbc);

        rightPanel.add(formCard);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        // ── Actions ───────────────────────────────────────────────────────
        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Authenticating...");
        statusLabel.setText(" ");

        SwingWorker<UIUser, Void> worker = new SwingWorker<>() {
            @Override protected UIUser doInBackground() {
                String hash = AppUtils.hashPassword(password);
                return facade.login(username, hash);
            }
            @Override protected void done() {
                try {
                    UIUser user = get();
                    loginButton.setEnabled(true);
                    loginButton.setText("  SIGN IN  ");
                    if (user != null) {
                        statusLabel.setForeground(LuxuryTheme.SUCCESS);
                        statusLabel.setText("Login successful!");
                        if (listener != null) listener.onLoginSuccess(user);
                    } else {
                        failCount++;
                        statusLabel.setForeground(LuxuryTheme.DANGER);
                        UIAuthExceptionSource exc = UIAuthExceptionSource.getInstance();
                        String ip = "localhost"; // replace with real IP if available
                        if (failCount >= 5) {
                            exc.fireAccountLocked(256, username, failCount);
                            statusLabel.setText("Account locked after too many attempts.");
                            loginButton.setEnabled(false);
                        } else {
                            exc.fireAuthFailed(252, username, ip, "INVALID_CREDENTIALS");
                            statusLabel.setText("Invalid credentials. Attempt " + failCount + "/5");
                        }
                        passwordField.setText("");
                    }
                } catch (Exception ex) {
                    loginButton.setEnabled(true);
                    loginButton.setText("  SIGN IN  ");
                    statusLabel.setForeground(LuxuryTheme.DANGER);
                    UIAuthExceptionSource.getInstance()
                        .fireAuthFailed(251, usernameField.getText().trim(), "localhost", "DB_CREDENTIALS_EXPIRED");
                    statusLabel.setText("Login error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
