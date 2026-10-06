package com.expensetracker.settings;

import com.expensetracker.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsTest {

    @Test
    void currencyFormatsWithSymbolAndGrouping() {
        BigDecimal amount = new BigDecimal("1234567.5");
        assertEquals("₹12,34,567.50", CurrencyOption.INR.format(amount));
        assertEquals("$1,234,567.50", CurrencyOption.USD.format(amount));
        assertEquals("€0.00", CurrencyOption.EUR.format(BigDecimal.ZERO));
    }

    @Test
    void dateStylesFormatAndParse() {
        LocalDate date = LocalDate.of(2026, 9, 6);
        assertEquals("2026-09-06", DateStyle.ISO.format(date));
        assertEquals("06/09/2026", DateStyle.DAY_FIRST.format(date));
        assertEquals("09/06/2026", DateStyle.MONTH_FIRST.format(date));
        assertEquals("06 Sep 2026", DateStyle.DAY_MONTH_NAME.format(date));

        assertEquals(date, DateStyle.DAY_FIRST.parse("6/9/2026"));
        assertEquals(date, DateStyle.MONTH_FIRST.parse("09/06/2026"));
        assertEquals(date, DateStyle.DAY_MONTH_NAME.parse("6 Sep 2026"));
    }

    @Test
    void isoIsAlwaysAcceptedWhenTyping() {
        assertEquals(LocalDate.of(2026, 9, 26), DateStyle.DAY_FIRST.parse("2026-09-26"));
    }

    @Test
    void invalidDateIsRejected() {
        assertThrows(DateTimeParseException.class, () -> DateStyle.DAY_FIRST.parse("31/02/2026"));
        assertThrows(DateTimeParseException.class, () -> DateStyle.ISO.parse("not a date"));
    }

    @Test
    void alertThresholdIsClamped() {
        assertEquals(50, UserSettings.defaults().withAlertThreshold(10).alertThreshold());
        assertEquals(100, UserSettings.defaults().withAlertThreshold(500).alertThreshold());
        assertEquals(75, UserSettings.defaults().withAlertThreshold(75).alertThreshold());
    }

    @Test
    void storePersistsAndResetsSettings() {
        SettingsStore store = new SettingsStore(UUID.randomUUID());
        try {
            assertEquals(UserSettings.defaults(), store.get());

            UserSettings changed = UserSettings.defaults()
                    .withCurrency(CurrencyOption.USD)
                    .withDateStyle(DateStyle.DAY_FIRST)
                    .withDefaultPayment(PaymentMethod.UPI)
                    .withDefaultCategory("Shopping")
                    .withOcrLanguage("hin")
                    .withAutoScan(false)
                    .withAlertThreshold(90);
            store.save(changed);
            assertEquals(changed, store.get());

            store.reset();
            assertEquals(UserSettings.defaults(), store.get());
            assertNull(store.get().defaultPayment());
        } finally {
            store.clear();
        }
    }

    @Test
    void storeNotifiesListeners() {
        SettingsStore store = new SettingsStore(UUID.randomUUID());
        try {
            UserSettings[] seen = new UserSettings[1];
            store.addListener(updated -> seen[0] = updated);
            store.save(UserSettings.defaults().withCurrency(CurrencyOption.GBP));
            assertEquals(CurrencyOption.GBP, seen[0].currency());
        } finally {
            store.clear();
        }
    }
}
