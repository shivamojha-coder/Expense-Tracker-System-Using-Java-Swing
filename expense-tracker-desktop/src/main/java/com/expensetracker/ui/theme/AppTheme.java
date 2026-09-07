package com.expensetracker.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;

public final class AppTheme {
    private AppTheme() {
    }

    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("defaultFont", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
    }
}