package com.expensetracker.settings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/** Currencies the user can display amounts in. Amounts are stored unchanged; this only affects display. */
public enum CurrencyOption {
    INR("₹", "Indian Rupee (₹)", true),
    USD("$", "US Dollar ($)", false),
    EUR("€", "Euro (€)", false),
    GBP("£", "British Pound (£)", false);

    private final String symbol;
    private final String label;
    private final boolean indianGrouping;

    CurrencyOption(String symbol, String label, boolean indianGrouping) {
        this.symbol = symbol;
        this.label = label;
        this.indianGrouping = indianGrouping;
    }

    public String symbol() {
        return symbol;
    }

    public String format(BigDecimal amount) {
        return symbol + (indianGrouping ? indianNumber(amount) : westernNumber(amount));
    }

    private static String westernNumber(BigDecimal amount) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount);
    }

    /** Lakh/crore grouping: the last three digits, then pairs (12,34,567.50). DecimalFormat cannot do this. */
    private static String indianNumber(BigDecimal amount) {
        String plain = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        boolean negative = plain.startsWith("-");
        if (negative) {
            plain = plain.substring(1);
        }
        int dot = plain.indexOf('.');
        String whole = plain.substring(0, dot);
        String fraction = plain.substring(dot);
        StringBuilder grouped;
        if (whole.length() <= 3) {
            grouped = new StringBuilder(whole);
        } else {
            grouped = new StringBuilder(whole.substring(whole.length() - 3));
            String rest = whole.substring(0, whole.length() - 3);
            for (int end = rest.length(); end > 0; end -= 2) {
                grouped.insert(0, rest.substring(Math.max(0, end - 2), end) + ",");
            }
        }
        return (negative ? "-" : "") + grouped + fraction;
    }

    @Override
    public String toString() {
        return label;
    }
}
