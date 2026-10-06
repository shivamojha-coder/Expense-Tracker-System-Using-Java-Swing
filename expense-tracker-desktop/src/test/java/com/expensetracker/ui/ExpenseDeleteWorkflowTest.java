package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ServiceException;
import com.expensetracker.service.StorageService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseDeleteWorkflowTest {
    private static final UUID USER = UUID.randomUUID();

    private static Expense expense(String receiptPath) {
        return new Expense(UUID.randomUUID(), USER, UUID.randomUUID(), "Shop", new BigDecimal("10"),
                LocalDate.of(2026, 9, 1), PaymentMethod.CASH, null, receiptPath,
                receiptPath == null ? ReceiptStatus.NO_RECEIPT : ReceiptStatus.AVAILABLE, Instant.now(), Instant.now());
    }

    private static final class FakeExpenses implements ExpenseService {
        final boolean fail;
        final List<UUID> deleted = new ArrayList<>();

        FakeExpenses(boolean fail) {
            this.fail = fail;
        }

        @Override
        public Expense createExpense(Expense expense) {
            return expense;
        }

        @Override
        public List<Expense> getExpenses(UUID userId) {
            return List.of();
        }

        @Override
        public Expense updateExpense(Expense expense) {
            return expense;
        }

        @Override
        public void deleteExpense(UUID expenseId) {
            if (fail) {
                throw new ServiceException("nope");
            }
            deleted.add(expenseId);
        }
    }

    private static final class FakeStorage implements StorageService {
        final boolean fail;
        final List<String> deleted = new ArrayList<>();

        FakeStorage(boolean fail) {
            this.fail = fail;
        }

        @Override
        public String uploadReceipt(UUID expenseId, Path localFile) {
            return "unused";
        }

        @Override
        public byte[] downloadReceipt(String receiptPath) {
            return new byte[0];
        }

        @Override
        public void deleteReceipt(String receiptPath) {
            if (fail) {
                throw new ServiceException("storage down");
            }
            deleted.add(receiptPath);
        }
    }

    @Test
    void deletingAnExpenseAlsoRemovesItsReceiptFile() {
        Expense withReceipt = expense("u/e/r.png");
        Expense other = expense(null);
        FakeStorage storage = new FakeStorage(false);

        ExpenseDeleteWorkflow.DeleteResult result = ExpenseDeleteWorkflow.delete(
                new FakeExpenses(false), storage, List.of(withReceipt, other), withReceipt.id());

        assertTrue(result.deleted());
        assertFalse(result.receiptLeftBehind());
        assertEquals(List.of("u/e/r.png"), storage.deleted);
        assertEquals(List.of(other), result.expenses());
    }

    @Test
    void expenseWithoutReceiptNeverTouchesStorage() {
        Expense plain = expense(null);
        FakeStorage storage = new FakeStorage(false);

        ExpenseDeleteWorkflow.DeleteResult result = ExpenseDeleteWorkflow.delete(
                new FakeExpenses(false), storage, List.of(plain), plain.id());

        assertTrue(result.deleted());
        assertTrue(storage.deleted.isEmpty());
    }

    @Test
    void failedReceiptRemovalIsReportedButExpenseStaysDeleted() {
        Expense withReceipt = expense("u/e/r.png");

        ExpenseDeleteWorkflow.DeleteResult result = ExpenseDeleteWorkflow.delete(
                new FakeExpenses(false), new FakeStorage(true), List.of(withReceipt), withReceipt.id());

        assertTrue(result.deleted());
        assertTrue(result.receiptLeftBehind());
        assertTrue(result.expenses().isEmpty());
    }

    @Test
    void failedExpenseDeleteKeepsRowAndLeavesReceiptAlone() {
        Expense withReceipt = expense("u/e/r.png");
        FakeStorage storage = new FakeStorage(false);

        ExpenseDeleteWorkflow.DeleteResult result = ExpenseDeleteWorkflow.delete(
                new FakeExpenses(true), storage, List.of(withReceipt), withReceipt.id());

        assertFalse(result.deleted());
        assertEquals(List.of(withReceipt), result.expenses());
        assertTrue(storage.deleted.isEmpty());
    }
}
