package com.expensetracker.settings;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** How dates are shown and typed. ISO (YYYY-MM-DD) is always accepted when typing, whatever style is chosen. */
public enum DateStyle {
    ISO("YYYY-MM-DD", "yyyy-MM-dd", "uuuu-M-d"),
    DAY_FIRST("DD/MM/YYYY", "dd/MM/yyyy", "d/M/uuuu"),
    MONTH_FIRST("MM/DD/YYYY", "MM/dd/yyyy", "M/d/uuuu"),
    DAY_MONTH_NAME("DD Mon YYYY", "dd MMM yyyy", "d MMM uuuu");

    private static final DateTimeFormatter ISO_PARSER = strict("uuuu-M-d");

    private final String hint;
    private final DateTimeFormatter formatter;
    private final DateTimeFormatter parser;

    DateStyle(String hint, String formatPattern, String parsePattern) {
        this.hint = hint;
        this.formatter = DateTimeFormatter.ofPattern(formatPattern, Locale.ENGLISH);
        this.parser = strict(parsePattern);
    }

    /** Strict parsing so impossible dates such as 31/02/2026 are rejected instead of being adjusted. */
    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    }

    /** Human-readable pattern such as {@code DD/MM/YYYY}. */
    public String hint() {
        return hint;
    }

    public String format(LocalDate date) {
        return formatter.format(date);
    }

    /** Parses text in this style, falling back to ISO. */
    public LocalDate parse(String text) {
        String value = text == null ? "" : text.trim();
        try {
            return LocalDate.parse(value, parser);
        } catch (DateTimeParseException first) {
            return LocalDate.parse(value, ISO_PARSER);
        }
    }

    @Override
    public String toString() {
        return hint + "  (e.g. " + format(LocalDate.of(2026, 9, 26)) + ")";
    }
}
