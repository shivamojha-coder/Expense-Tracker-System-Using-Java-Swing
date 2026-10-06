package com.expensetracker.ui;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.User;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.ServiceException;
import com.expensetracker.service.SupabaseAuthService;
import com.expensetracker.supabase.SupabaseClient;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

public final class MainWindow extends JFrame {
    private static final String CARD_LANDING = "landing";
    private static final String CARD_AUTH = "auth";
    private static final String CARD_REGISTER = "register";

    private final AppConfig config;
    private final AuthService authService;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootCardPanel = new JPanel(cardLayout);

    private AuthPage loginPage;
    private AuthPage registerPage;

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
        LandingPage landingPage = new LandingPage(
                () -> showCard(CARD_REGISTER), // On Sign Up / Get Started
                () -> showCard(CARD_AUTH)      // On Login
        );

        loginPage = new AuthPage(AuthPage.Mode.LOGIN, config.isSupabaseConfigured(), new AuthPage.Actions(
                this::handleLogin,
                () -> showCard(CARD_REGISTER),
                () -> showCard(CARD_LANDING),
                this::handleForgotPassword));
        registerPage = new AuthPage(AuthPage.Mode.REGISTER, config.isSupabaseConfigured(), new AuthPage.Actions(
                this::handleRegister,
                () -> showCard(CARD_AUTH),
                () -> showCard(CARD_LANDING),
                null));

        rootCardPanel.add(landingPage, CARD_LANDING);
        rootCardPanel.add(loginPage, CARD_AUTH);
        rootCardPanel.add(registerPage, CARD_REGISTER);

        setLayout(new BorderLayout());
        add(rootCardPanel, BorderLayout.CENTER);

        showCard(CARD_LANDING);
    }

    private void showCard(String card) {
        cardLayout.show(rootCardPanel, card);
    }

    private void handleLogin() {
        if (loginPage.email().isBlank() || loginPage.password().length == 0) {
            JOptionPane.showMessageDialog(this, "Enter your email and password.", "Missing Details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!config.isSupabaseConfigured()) {
            offerDemoMode();
            return;
        }
        runAuthOperation("Signing in...", () -> authService.login(loginPage.email().trim(), loginPage.password()));
    }

    private void handleRegister() {
        if (registerPage.email().isBlank() || registerPage.password().length == 0) {
            JOptionPane.showMessageDialog(this, "Enter an email and password to create an account.", "Missing Details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!config.isSupabaseConfigured()) {
            offerDemoMode();
            return;
        }
        runRegisterOperation();
    }

    private void handleForgotPassword() {
        String email = loginPage.email().trim();
        if (email.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Enter your email address in the field above, then click \"Forgot password?\" again.",
                    "Reset password", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!config.isSupabaseConfigured()) {
            JOptionPane.showMessageDialog(this,
                    "Password reset needs the cloud service, which is not configured.\nUse Offline Demo Mode instead.",
                    "Reset password", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        loginPage.setBusy(true, "Sending reset email...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                authService.sendPasswordReset(email);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loginPage.setBusy(false, " ");
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "If an account exists for " + email + ", a password reset link has been sent.",
                            "Check your email", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException && exception.getCause() != null
                            ? exception.getCause() : exception;
                    String message = cause instanceof ServiceException ? cause.getMessage() : "Unable to send the password reset email.";
                    loginPage.setBusy(false, message);
                    JOptionPane.showMessageDialog(MainWindow.this, message, "Reset password", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void offerDemoMode() {
        int opt = JOptionPane.showConfirmDialog(
                this,
                "Supabase configuration is not detected.\nWould you like to explore the application in Offline Demo Mode?",
                "Launch Demo Mode",
                JOptionPane.YES_NO_OPTION
        );
        if (opt == JOptionPane.YES_OPTION) {
            launchDemoMode();
        }
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
        loginPage.setBusy(true, status);
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
                    loginPage.setBusy(false, message);
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
        String email = registerPage.email().trim();
        char[] password = registerPage.password();
        registerPage.setBusy(true, "Creating your account...");
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
                        registerPage.setBusy(false, " ");
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
                    registerPage.setBusy(false, message);
                    JOptionPane.showMessageDialog(MainWindow.this, message, "Registration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openAppFrame(User user) {
        // Leave both forms ready for the next visit (after logout the same pages are shown again).
        loginPage.setBusy(false, " ");
        registerPage.setBusy(false, " ");
        loginPage.clearPassword();
        registerPage.clearPassword();
        setVisible(false);
        AppFrame appFrame = new AppFrame(config, user, authService);
        appFrame.setOnLogoutCallback(() -> {
            setVisible(true);
            showCard(CARD_LANDING);
        });
        appFrame.setVisible(true);
    }

    @FunctionalInterface
    private interface AuthOperation {
        User run();
    }
}
