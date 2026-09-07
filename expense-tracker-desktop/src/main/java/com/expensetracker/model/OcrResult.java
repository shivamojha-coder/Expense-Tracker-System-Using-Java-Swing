package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OcrResult(
        String merchant,
        BigDecimal amount,
        LocalDate expenseDate,
        String rawText
) {
}