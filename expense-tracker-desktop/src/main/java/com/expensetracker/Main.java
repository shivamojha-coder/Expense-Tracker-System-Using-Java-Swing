package com.expensetracker;

import com.expensetracker.config.AppConfig;
import com.expensetracker.ui.LoginFrame;
import com.expensetracker.ui.theme.AppTheme;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        AppTheme.install();
        SwingUtilities.invokeLater(() -> new LoginFrame(AppConfig.fromEnvironment()).setVisible(true));
    }
}