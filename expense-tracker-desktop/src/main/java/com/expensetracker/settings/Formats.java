package com.expensetracker.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Formats and parses money and dates according to the user's settings. */
public final class Formats {
    private final UserSettings settings;

    public Formats(UserSettings settings) {
        this.settings = settings;
    }

    public String money(BigDecimal amount) {
        return settings.currency().format(amount);
    }

    public String currencySymbol() {
        return settings.currency().symbol();
    }

    public String date(LocalDate date) {
        return settings.dateStyle().format(date);
    }

    /** @throws DateTimeParseException when the text is neither in the chosen style nor ISO */
    public LocalDate parseDate(String text) {
        return settings.dateStyle().parse(text);
    }

    public String dateHint() {
        return settings.dateStyle().hint();
    }
}
