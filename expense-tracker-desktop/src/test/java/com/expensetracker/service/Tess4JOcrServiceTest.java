package com.expensetracker.service;

import com.expensetracker.model.OcrResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class Tess4JOcrServiceTest {

    @Test
    void testParseTextWithLabeledTotalAndIsoDate() {
        String receiptText = """
                WALMART SUPERCENTER #1234
                100 MAIN STREET
                DATE: 2026-03-12
                ITEMS:
                MILK 1GAL        $3.49
                BREAD WHOLE WHEAT $2.50
                SUBTOTAL         $5.99
                TAX              $0.48
                TOTAL DUE:      $6.47
                CASH TENDERED   $10.00
                CHANGE           $3.53
                """;

        OcrResult result = Tess4JOcrService.parseText(receiptText);

        assertEquals("WALMART SUPERCENTER #1234", result.merchant());
        assertEquals(new BigDecimal("6.47"), result.amount());
        assertEquals(LocalDate.of(2026, 3, 12), result.expenseDate());
    }

    @Test
    void testParseTextWithMonthNameDate() {
        String receiptText = """
                TARGET STORE #552
                March 15 2026
                ITEMS:
                COFFEE MAKER     $49.99
                TOTAL            $49.99
                """;

        OcrResult result = Tess4JOcrService.parseText(receiptText);

        assertEquals("TARGET STORE #552", result.merchant());
        assertEquals(new BigDecimal("49.99"), result.amount());
        assertEquals(LocalDate.of(2026, 3, 15), result.expenseDate());
    }

    @Test
    void testParseTextEmptyOrNullGracefulHandling() {
        OcrResult result = Tess4JOcrService.parseText("");
        assertNull(result.merchant());
        assertNull(result.amount());
        assertNull(result.expenseDate());
        assertEquals("", result.rawText());

        OcrResult nullResult = Tess4JOcrService.parseText(null);
        assertNull(nullResult.merchant());
        assertNull(nullResult.amount());
        assertNull(nullResult.expenseDate());
        assertEquals("", nullResult.rawText());
    }
}
