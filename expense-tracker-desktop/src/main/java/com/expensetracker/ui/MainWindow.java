package com.expensetracker.ui;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.User;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.ServiceException;
import com.expensetracker.service.SupabaseAuthService;
import com.expensetracker.supabase.SupabaseClient;
import com.expensetracker.ui.theme.ThemeColors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class MainWindow extends JFrame {
    private static final String CARD_LANDING = "landing";
    private static final String CARD_AUTH = "auth";
    private static final String CARD_REGISTER = "register";

    private final AppConfig config;
    private final AuthService authService;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootCardPanel = new JPanel(cardLayout);

    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("Log in");
    private final JButton registerButton = new JButton("New here? Create an account");
    private final JButton demoModeButton = new JButton("✦ Explore in Offline Demo Mode");
    private final JLabel statusLabel = new JLabel(" ");

    private final JTextField registerEmailField = new JTextField();
    private final JPasswordField registerPasswordField = new JPasswordField();
    private final JButton registerSubmitButton = new JButton("Create account");
    private final JButton backToLoginButton = new JButton("Already have an account? Log in");
    private final JButton registerDemoModeButton = new JButton("✦ Explore in Offline Demo Mode");
    private final JLabel registerStatusLabel = new JLabel(" ");

    public MainWindow() {
        this(AppConfig.fromEnvironment());
    }

    public MainWindow(AppConfig config) {
        this(config, new SupabaseAuthService(new SupabaseClient(config)));
    }

    public MainWindow(AppConfig config, AuthService authService) {
        super("Expense Tracker");
        this.config = config;
        this.authService = authService;
        configureFrame();
        buildUi();
    }

    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1240, 800);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        // 1. Landing Page Card
        LandingPage landingPage = new LandingPage(
                () -> showCard(CARD_REGISTER), // On Sign Up / Get Started
                () -> showCard(CARD_AUTH)      // On Login
        );

        // 2. Auth Page Cards
        JPanel authPanel = buildAuthPanel();
        JPanel registerPanel = buildRegisterPanel();

        rootCardPanel.add(landingPage, CARD_LANDING);
        rootCardPanel.add(authPanel, CARD_AUTH);
        rootCardPanel.add(registerPanel, CARD_REGISTER);

        setLayout(new BorderLayout());
        add(rootCardPanel, BorderLayout.CENTER);

        showCard(CARD_LANDING);
    }

    private void showCard(String card) {
        cardLayout.show(rootCardPanel, card);
    }

    private JPanel buildAuthPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ThemeColors.BACKGROUND);

        // Top mini navigation for Auth screen
        JPanel topNav = new JPanel(new BorderLayout());
        topNav.setBackground(ThemeColors.WHITE);
        topNav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));

        JButton backBtn = new JButton("← Back to Overview");
        backBtn.setFont(ThemeColors.FONT_BODY);
        backBtn.setForeground(ThemeColors.DARK_GREEN);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> showCard(CARD_LANDING));

        topNav.add(backBtn, BorderLayout.WEST);
        wrapper.add(topNav, BorderLayout.NORTH);

        // Center Login Card
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(ThemeColors.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));
        card.setPreferredSize(new Dimension(440, 520));

        // Heading
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("EXPENSE TRACKER");
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        brand.setForeground(ThemeColors.DARK_GREEN);

        JLabel subtitle = new JLabel("Sign in to your private financial workspace");
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setFont(ThemeColors.FONT_SMALL);
        subtitle.setForeground(ThemeColors.SECONDARY_TEXT);

        heading.add(brand);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);
        card.add(heading, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        addFormField(form, c, 0, "Email address", emailField);
        addFormField(form, c, 2, "Password", passwordField);

        loginButton.setPreferredSize(new Dimension(0, 42));
        loginButton.setBackground(ThemeColors.GREEN);
        loginButton.setForeground(ThemeColors.WHITE);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        loginButton.setFocusPainted(false);
        loginButton.addActionListener(event -> handleLogin());

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.insets = new Insets(18, 0, 8, 0);
        form.add(loginButton, c);

        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setForeground(ThemeColors.GREEN);
        registerButton.setFont(ThemeColors.FONT_BODY);
        registerButton.setFocusPainted(false);
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(event -> showCard(CARD_REGISTER));

        c.gridy = 5;
        c.insets = new Insets(2, 0, 10, 0);
        form.add(registerButton, c);

        // Demo Mode Button
        demoModeButton.setPreferredSize(new Dimension(0, 38));
        demoModeButton.setBackground(new Color(0xE6, 0xF7, 0xF0));
        demoModeButton.setForeground(ThemeColors.DARK_GREEN);
        demoModeButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        demoModeButton.setBorder(BorderFactory.createLineBorder(new Color(0xA7, 0xE9, 0xCD), 1, true));
        demoModeButton.setFocusPainted(false);
        demoModeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        demoModeButton.addActionListener(event -> launchDemoMode());

        c.gridy = 6;
        c.insets = new Insets(4, 0, 0, 0);
        form.add(demoModeButton, c);

        card.add(form, BorderLayout.CENTER);

        // Status & Config hint
        JLabel configHint = new JLabel(
                config.isSupabaseConfigured()
                        ? "● Cloud service connected"
                        : "Cloud config not detected — use Offline Demo Mode or set SUPABASE_URL"
        );
        configHint.setHorizontalAlignment(JLabel.CENTER);
        configHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        configHint.setForeground(config.isSupabaseConfigured() ? ThemeColors.SUCCESS : new Color(180, 83, 9));

        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setForeground(new Color(180, 83, 9));

        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.add(statusLabel, BorderLayout.NORTH);
        footer.add(configHint, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);

        center.add(card);
        wrapper.add(center, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildRegisterPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ThemeColors.BACKGROUND);

        JPanel topNav = new JPanel(new BorderLayout());
        topNav.setBackground(ThemeColors.WHITE);
        topNav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));

        JButton backBtn = new JButton("← Back to Overview");
        backBtn.setFont(ThemeColors.FONT_BODY);
        backBtn.setForeground(ThemeColors.DARK_GREEN);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> showCard(CARD_LANDING));

        topNav.add(backBtn, BorderLayout.WEST);
        wrapper.add(topNav, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(ThemeColors.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeColors.BORDER, 1, true),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));
        card.setPreferredSize(new Dimension(440, 520));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("EXPENSE TRACKER");
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        brand.setForeground(ThemeColors.DARK_GREEN);

        JLabel subtitle = new JLabel("Create your private financial workspace");
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setFont(ThemeColors.FONT_SMALL);
        subtitle.setForeground(ThemeColors.SECONDARY_TEXT);

        heading.add(brand);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);
        card.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        addFormField(form, c, 0, "Email address", registerEmailField);
        addFormField(form, c, 2, "Password", registerPasswordField);

        registerSubmitButton.setPreferredSize(new Dimension(0, 42));
        registerSubmitButton.setBackground(ThemeColors.GREEN);
        registerSubmitButton.setForeground(ThemeColors.WHITE);
        registerSubmitButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        registerSubmitButton.setFocusPainted(false);
        registerSubmitButton.addActionListener(event -> handleRegister());

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.insets = new Insets(18, 0, 8, 0);
        form.add(registerSubmitButton, c);

        backToLoginButton.setBorderPainted(false);
        backToLoginButton.setContentAreaFilled(false);
        backToLoginButton.setForeground(ThemeColors.GREEN);
        backToLoginButton.setFont(ThemeColors.FONT_BODY);
        backToLoginButton.setFocusPainted(false);
        backToLoginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToLoginButton.addActionListener(event -> showCard(CARD_AUTH));

        c.gridy = 5;
        c.insets = new Insets(2, 0, 10, 0);
        form.add(backToLoginButton, c);

        registerDemoModeButton.setPreferredSize(new Dimension(0, 38));
        registerDemoModeButton.setBackground(new Color(0xE6, 0xF7, 0xF0));
        registerDemoModeButton.setForeground(ThemeColors.DARK_GREEN);
        registerDemoModeButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        registerDemoModeButton.setBorder(BorderFactory.createLineBorder(new Color(0xA7, 0xE9, 0xCD), 1, true));
        registerDemoModeButton.setFocusPainted(false);
        registerDemoModeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerDemoModeButton.addActionListener(event -> launchDemoMode());

        c.gridy = 6;
        c.insets = new Insets(4, 0, 0, 0);
        form.add(registerDemoModeButton, c);

        card.add(form, BorderLayout.CENTER);

        JLabel configHint = new JLabel(
                config.isSupabaseConfigured()
                        ? "● Cloud service connected"
                        : "Cloud config not detected — use Offline Demo Mode or set SUPABASE_URL"
        );
        configHint.setHorizontalAlignment(JLabel.CENTER);
        configHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        configHint.setForeground(config.isSupabaseConfigured() ? ThemeColors.SUCCESS : new Color(180, 83, 9));

        registerStatusLabel.setHorizontalAlignment(JLabel.CENTER);
        registerStatusLabel.setForeground(new Color(180, 83, 9));

        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.add(registerStatusLabel, BorderLayout.NORTH);
        footer.add(configHint, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);

        center.add(card);
        wrapper.add(center, BorderLayout.CENTER);
        return wrapper;
    }

    private void addFormField(JPanel form, GridBagConstraints template, int row, String label, javax.swing.JComponent field) {
        GridBagConstraints lc = (GridBagConstraints) template.clone();
        lc.gridx = 0;
        lc.gridy = row;
        lc.gridwidth = 2;
        lc.insets = new Insets(6, 0, 2, 0);
        JLabel l = new JLabel(label);
        l.setFont(ThemeColors.FONT_BODY);
        l.setForeground(ThemeColors.PRIMARY_TEXT);
        form.add(l, lc);

        GridBagConstraints fc = (GridBagConstraints) template.clone();
        fc.gridx = 0;
        fc.gridy = row + 1;
        fc.gridwidth = 2;
        fc.insets = new Insets(2, 0, 6, 0);
        field.setPreferredSize(new Dimension(0, 38));
        form.add(field, fc);
    }

    private void handleLogin() {
        if (emailField.getText().isBlank() || passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Enter your email and password.", "Missing Details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!config.isSupabaseConfigured()) {
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Supabase configuration is not detected.\nWould you like to explore the application in Offline Demo Mode?",
                    "Launch Demo Mode",
                    JOptionPane.YES_NO_OPTION
            );
            if (opt == JOptionPane.YES_OPTION) {
                launchDemoMode();
            }
            return;
        }
        runAuthOperation("Signing in...", () -> authService.login(emailField.getText().trim(), passwordField.getPassword()));
    }

    private void handleRegister() {
        if (registerEmailField.getText().isBlank() || registerPasswordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Enter an email and password to create an account.", "Missing Details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!config.isSupabaseConfigured()) {
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Supabase configuration is not detected.\nWould you like to explore the application in Offline Demo Mode?",
                    "Launch Demo Mode",
                    JOptionPane.YES_NO_OPTION
            );
            if (opt == JOptionPane.YES_OPTION) {
                launchDemoMode();
            }
            return;
        }
        runRegisterOperation();
    }

    private void launchDemoMode() {
        setVisible(false);
        AppFrame demoFrame = AppFrame.createDemo(config, () -> {
            setVisible(true);
            showCard(CARD_LANDING);
        });
        demoFrame.setVisible(true);
    }

    private void runAuthOperation(String status, AuthOperation operation) {
        setBusy(true, status);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return operation.run();
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    openAppFrame(user);
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException && exception.getCause() != null
                            ? exception.getCause() : exception;
                    String message = cause instanceof ServiceException ? cause.getMessage() : "Authentication failed. Please check credentials.";
                    setBusy(false, message);
                    JOptionPane.showMessageDialog(MainWindow.this, message, "Authentication Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Registration is handled separately from login because Supabase does not
     * return a session when the project requires email confirmation (the
     * default for new projects). In that case the signed-up user exists but
     * has no access token yet, so opening AppFrame would immediately fail
     * every request with "session has expired".
     */
    private void runRegisterOperation() {
        String email = registerEmailField.getText().trim();
        char[] password = registerPasswordField.getPassword();
        setRegisterBusy(true, "Creating your account...");
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.register(email, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (authService.getAccessToken() == null || authService.getAccessToken().isBlank()) {
                        setRegisterBusy(false, " ");
                        JOptionPane.showMessageDialog(
                                MainWindow.this,
                                "Account created for " + user.email() + ".\n"
                                        + "Check your email to confirm the address, then sign in.\n"
                                        + "If you already had an account with this email, just sign in instead.",
                                "Confirm your email",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        showCard(CARD_AUTH);
                        return;
                    }
                    openAppFrame(user);
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException && exception.getCause() != null
                            ? exception.getCause() : exception;
                    String message = cause instanceof ServiceException ? cause.getMessage() : "Unable to create the account.";
                    setRegisterBusy(false, message);
                    JOptionPane.showMessageDialog(MainWindow.this, message, "Registration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openAppFrame(User user) {
        setVisible(false);
        AppFrame appFrame = new AppFrame(config, user, authService);
        appFrame.setOnLogoutCallback(() -> {
            setVisible(true);
            showCard(CARD_LANDING);
        });
        appFrame.setVisible(true);
    }

    private void setBusy(boolean busy, String status) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
        demoModeButton.setEnabled(!busy);
        emailField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        statusLabel.setText(status);
    }

    private void setRegisterBusy(boolean busy, String status) {
        registerSubmitButton.setEnabled(!busy);
        backToLoginButton.setEnabled(!busy);
        registerDemoModeButton.setEnabled(!busy);
        registerEmailField.setEnabled(!busy);
        registerPasswordField.setEnabled(!busy);
        registerStatusLabel.setText(status);
    }

    @FunctionalInterface
    private interface AuthOperation {
        User run();
    }
}
