package com.expensetracker.ui;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.User;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.ServiceException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class LoginFrame extends JFrame {
    private final AppConfig config;
    private final AuthService authService;
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("Log in");
    private final JButton registerButton = new JButton("Create an account");
    private final JLabel statusLabel = new JLabel(" ");

    public LoginFrame(AppConfig config, AuthService authService) {
        super("Expense Tracker");
        this.config = config;
        this.authService = authService;
        configureFrame();
        buildUi();
    }

    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setSize(1080, 700);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(241, 245, 249));

        JPanel card = new JPanel(new BorderLayout(0, 26));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(42, 46, 42, 46)
        ));
        card.setPreferredSize(new Dimension(420, 480));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("EXPENSE TRACKER");
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 24f));
        brand.setForeground(new Color(15, 118, 110));
        JLabel subtitle = new JLabel("A clearer view of your everyday spending");
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setForeground(new Color(100, 116, 139));
        heading.add(brand);
        heading.add(javax.swing.Box.createVerticalStrut(8));
        heading.add(subtitle);
        card.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(7, 0, 7, 0);

        addFormField(form, c, 0, "Email", emailField);
        addFormField(form, c, 2, "Password", passwordField);

        loginButton.setPreferredSize(new Dimension(0, 42));
        loginButton.setBackground(new Color(15, 118, 110));
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(event -> handleLogin());
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.insets = new Insets(22, 0, 8, 0);
        form.add(loginButton, c);

        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setForeground(new Color(15, 118, 110));
        registerButton.addActionListener(event -> handleRegister());
        c.gridy = 5;
        c.insets = new Insets(4, 0, 0, 0);
        form.add(registerButton, c);
        card.add(form, BorderLayout.CENTER);

        JLabel configurationHint = new JLabel(
                config.isSupabaseConfigured()
                        ? "Connected configuration detected"
                        : "Supabase configuration is required before signing in"
        );
        configurationHint.setHorizontalAlignment(JLabel.CENTER);
        configurationHint.setForeground(config.isSupabaseConfigured()
                ? new Color(22, 101, 52)
                : new Color(180, 83, 9));
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setForeground(new Color(180, 83, 9));
        JPanel footer = new JPanel(new java.awt.BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(statusLabel, java.awt.BorderLayout.NORTH);
        footer.add(configurationHint, java.awt.BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);

        GridBagConstraints rootConstraints = new GridBagConstraints();
        rootConstraints.gridx = 0;
        rootConstraints.gridy = 0;
        root.add(card, rootConstraints);
        add(root);
    }

    private void addFormField(JPanel form, GridBagConstraints template, int row, String label, javax.swing.JComponent field) {
        GridBagConstraints labelConstraints = (GridBagConstraints) template.clone();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.gridwidth = 2;
        labelConstraints.insets = new Insets(7, 0, 2, 0);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(new Color(51, 65, 85));
        field.addPropertyChangeListener("ancestor", event -> field.setToolTipText(label));
        form.add(fieldLabel, labelConstraints);

        GridBagConstraints fieldConstraints = (GridBagConstraints) template.clone();
        fieldConstraints.gridx = 0;
        fieldConstraints.gridy = row + 1;
        fieldConstraints.gridwidth = 2;
        fieldConstraints.insets = new Insets(2, 0, 7, 0);
        field.setPreferredSize(new Dimension(0, 40));
        form.add(field, fieldConstraints);
    }

    private void handleLogin() {
        if (emailField.getText().isBlank() || passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Enter your email and password.", "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!config.isSupabaseConfigured()) {
            showNotReadyMessage("Supabase is not configured yet. Set SUPABASE_URL and SUPABASE_ANON_KEY before connecting authentication.");
            return;
        }
        runAuthOperation("Signing in...", () -> authService.login(emailField.getText().trim(), passwordField.getPassword()));
    }

    private void handleRegister() {
        if (emailField.getText().isBlank() || passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Enter an email and password to create your account.", "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!config.isSupabaseConfigured()) {
            showNotReadyMessage("Supabase is not configured yet. Set SUPABASE_URL and SUPABASE_ANON_KEY before creating an account.");
            return;
        }
        runAuthOperation("Creating your account...", () -> authService.register(emailField.getText().trim(), passwordField.getPassword()));
    }

    private void runAuthOperation(String status, AuthOperation operation) {
        setBusy(true, status);
        new javax.swing.SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return operation.run();
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    dispose();
                    new AppFrame(config, user, authService).setVisible(true);
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = cause instanceof ServiceException
                            ? cause.getMessage()
                            : "Authentication failed. Please try again.";
                    setBusy(false, message);
                    JOptionPane.showMessageDialog(LoginFrame.this, message, "Authentication failed", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setBusy(false, "Authentication was interrupted.");
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy, String status) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
        emailField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        statusLabel.setText(status);
    }

    private void showNotReadyMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Expense Tracker", JOptionPane.INFORMATION_MESSAGE);
    }

    @FunctionalInterface
    private interface AuthOperation {
        User run();
    }
}