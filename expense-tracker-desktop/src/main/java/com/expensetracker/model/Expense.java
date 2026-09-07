package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Expense(
        UUID id,
        UUID userId,
        UUID categoryId,
        String merchant,
        BigDecimal amount,
        LocalDate expenseDate,
        PaymentMethod paymentMethod,
        String description,
        String receiptPath,
        ReceiptStatus receiptStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public Expense {
        if (merchant == null || merchant.isBlank()) {
            throw new IllegalArgumentException("Merchant must not be blank");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (expenseDate == null) {
            throw new IllegalArgumentException("Expense date is required");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (receiptStatus == null) {
            throw new IllegalArgumentException("Receipt status is required");
        }
    }
}