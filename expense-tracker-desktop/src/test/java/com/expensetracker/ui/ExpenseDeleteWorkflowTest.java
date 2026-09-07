package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExpenseDeleteWorkflowTest {
    @Test
    void failedDeleteKeepsRowAndExposesError() {
        Expense expense = expense();
        ServiceException failure = new ServiceException("The expense was not found.");
        ExpenseService service = new StubExpenseService(failure);

        ExpenseDeleteWorkflow.DeleteResult result =
                ExpenseDeleteWorkflow.delete(service, List.of(expense), expense.id());

        assertFalse(result.deleted());
        assertEquals(List.of(expense), result.expenses());
        assertSame(failure, result.error());
        assertEquals("The expense was not found.", result.error().getMessage());
    }

    private Expense expense() {
        return new Expense(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Coffee",
                BigDecimal.TEN,
                LocalDate.of(2026, 9, 7),
                PaymentMethod.CASH,
                null,
                null,
                ReceiptStatus.NO_RECEIPT,
                null,
                null
        );
    }

    private static final class StubExpenseService implements ExpenseService {
        private final RuntimeException failure;

        private StubExpenseService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public Expense createExpense(Expense expense) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Expense> getExpenses(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Expense updateExpense(Expense expense) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteExpense(UUID expenseId) {
            throw failure;
        }

        @Override
        public List<Expense> searchExpenses(UUID userId, String query) {
            throw new UnsupportedOperationException();
        }
    }
}