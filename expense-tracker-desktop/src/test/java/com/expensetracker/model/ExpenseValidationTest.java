package com.expensetracker.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpenseValidationTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> expense(BigDecimal.ZERO, LocalDate.now(), CATEGORY_ID, PaymentMethod.CASH)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> expense(new BigDecimal("-1"), LocalDate.now(), CATEGORY_ID, PaymentMethod.CASH)
        );
    }

    @Test
    void requiresExpenseDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> expense(BigDecimal.ONE, null, CATEGORY_ID, PaymentMethod.CASH)
        );
    }

    @Test
    void requiresCategory() {
        assertThrows(
                IllegalArgumentException.class,
                () -> expense(BigDecimal.ONE, LocalDate.now(), null, PaymentMethod.CASH)
        );
    }

    @Test
    void requiresPaymentMethod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> expense(BigDecimal.ONE, LocalDate.now(), CATEGORY_ID, null)
        );
    }

    private Expense expense(
            BigDecimal amount,
            LocalDate date,
            UUID categoryId,
            PaymentMethod paymentMethod
    ) {
        return new Expense(
                UUID.randomUUID(),
                USER_ID,
                categoryId,
                "Coffee",
                amount,
                date,
                paymentMethod,
                null,
                null,
                ReceiptStatus.NO_RECEIPT,
                null,
                null
        );
    }
}