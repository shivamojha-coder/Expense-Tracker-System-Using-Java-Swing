package com.expensetracker.service;

import com.expensetracker.model.OcrResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptAndReportTest {

    @Test
    void csvCellsThatLookLikeFormulasAreNeutralised() {
        assertEquals("'=SUM(A1)", DefaultReportService.escapeCsv("=SUM(A1)"));
        assertEquals("'+1", DefaultReportService.escapeCsv("+1"));
        assertEquals("'@cmd", DefaultReportService.escapeCsv("@cmd"));
        assertEquals("'-5", DefaultReportService.escapeCsv("-5"));
    }

    @Test
    void csvEscapingStillQuotesSpecialCharacters() {
        assertEquals("plain", DefaultReportService.escapeCsv("plain"));
        assertEquals("\"a,b\"", DefaultReportService.escapeCsv("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", DefaultReportService.escapeCsv("say \"hi\""));
        assertEquals("", DefaultReportService.escapeCsv(null));
        assertEquals("\"'=1,2\"", DefaultReportService.escapeCsv("=1,2"));
    }

    @Test
    void ocrReadsRupeeAndRsTotals() {
        assertEquals(0, new BigDecimal("340.00").compareTo(
                Tess4JOcrService.parseText("Coffee House\nTotal ₹340.00").amount()));
        assertEquals(0, new BigDecimal("1250").compareTo(
                Tess4JOcrService.parseText("Grand Total: Rs. 1,250").amount()));
        assertEquals(0, new BigDecimal("89.5").compareTo(
                Tess4JOcrService.parseText("Amount due INR 89.50").amount()));
    }

    @Test
    void ocrReadsDayFirstAndNamedMonthDates() {
        assertEquals(LocalDate.of(2026, 9, 26), Tess4JOcrService.parseText("Date 26/09/2026").expenseDate());
        assertEquals(LocalDate.of(2026, 9, 26), Tess4JOcrService.parseText("26 Sep 2026").expenseDate());
        assertEquals(LocalDate.of(2026, 9, 26), Tess4JOcrService.parseText("26th September, 2026").expenseDate());
        assertEquals(LocalDate.of(2026, 9, 26), Tess4JOcrService.parseText("Sep 26, 2026").expenseDate());
        assertEquals(LocalDate.of(2026, 9, 26), Tess4JOcrService.parseText("2026-09-26").expenseDate());
    }

    @Test
    void ocrRejectsImpossibleDates() {
        OcrResult result = Tess4JOcrService.parseText("31/02/2026");
        assertNull(result.expenseDate());
    }
}
