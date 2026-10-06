package com.expensetracker.settings;

import com.expensetracker.model.PaymentMethod;

/**
 * Per-user preferences kept on this computer.
 *
 * @param defaultPayment  payment method preselected for new expenses, or null for none
 * @param defaultCategory category name preselected for new expenses, or null for none
 * @param ocrLanguage     Tesseract language code used when reading receipts
 * @param autoScan        read receipts automatically when they are attached
 * @param alertThreshold  percentage of a budget at which a warning is shown (50-100)
 */
public record UserSettings(
        CurrencyOption currency,
        DateStyle dateStyle,
        PaymentMethod defaultPayment,
        String defaultCategory,
        String ocrLanguage,
        boolean autoScan,
        int alertThreshold
) {
    public static UserSettings defaults() {
        return new UserSettings(CurrencyOption.INR, DateStyle.ISO, null, null, "eng", true, 80);
    }

    public UserSettings withCurrency(CurrencyOption value) {
        return new UserSettings(value, dateStyle, defaultPayment, defaultCategory, ocrLanguage, autoScan, alertThreshold);
    }

    public UserSettings withDateStyle(DateStyle value) {
        return new UserSettings(currency, value, defaultPayment, defaultCategory, ocrLanguage, autoScan, alertThreshold);
    }

    public UserSettings withDefaultPayment(PaymentMethod value) {
        return new UserSettings(currency, dateStyle, value, defaultCategory, ocrLanguage, autoScan, alertThreshold);
    }

    public UserSettings withDefaultCategory(String value) {
        return new UserSettings(currency, dateStyle, defaultPayment, value, ocrLanguage, autoScan, alertThreshold);
    }

    public UserSettings withOcrLanguage(String value) {
        return new UserSettings(currency, dateStyle, defaultPayment, defaultCategory, value, autoScan, alertThreshold);
    }

    public UserSettings withAutoScan(boolean value) {
        return new UserSettings(currency, dateStyle, defaultPayment, defaultCategory, ocrLanguage, value, alertThreshold);
    }

    public UserSettings withAlertThreshold(int value) {
        return new UserSettings(currency, dateStyle, defaultPayment, defaultCategory, ocrLanguage, autoScan,
                Math.max(50, Math.min(100, value)));
    }
}
