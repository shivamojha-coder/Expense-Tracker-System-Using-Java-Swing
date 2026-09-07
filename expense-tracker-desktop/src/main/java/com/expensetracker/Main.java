package com.expensetracker;

import com.expensetracker.config.AppConfig;
import com.expensetracker.service.SupabaseAuthService;
import com.expensetracker.supabase.SupabaseClient;
import com.expensetracker.ui.LoginFrame;
import com.expensetracker.ui.theme.AppTheme;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        AppTheme.install();
        AppConfig config = AppConfig.fromEnvironment();
        SupabaseAuthService authService = new SupabaseAuthService(new SupabaseClient(config));
        SwingUtilities.invokeLater(() -> new LoginFrame(config, authService).setVisible(true));
    }
}