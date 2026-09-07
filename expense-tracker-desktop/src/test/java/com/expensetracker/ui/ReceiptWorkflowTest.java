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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptWorkflowTest {
    private final UUID userId = UUID.randomUUID();

    @Test
    void attachesNewReceiptAndPersistsAvailableMetadata() {
        Expense saved = expense(null, ReceiptStatus.NO_RECEIPT);
        RecordingStorage storage = new RecordingStorage("new/receipt.jpg");
        RecordingExpenseService expenses = new RecordingExpenseService();

        ReceiptWorkflow.Result result = ReceiptWorkflow.finish(
                expenses, storage, saved, null, Path.of("receipt.jpg"), false
        );

        assertFalse(result.warning());
        assertEquals(ReceiptStatus.AVAILABLE, expenses.lastUpdated.receiptStatus());
        assertEquals("new/receipt.jpg", expenses.lastUpdated.receiptPath());
        assertEquals(List.of(), storage.deletedPaths);
    }

    @Test
    void replacesReceiptAndCleansUpOldObjectAfterMetadataUpdate() {
        Expense existing = expense("old/receipt.jpg", ReceiptStatus.AVAILABLE);
        RecordingStorage storage = new RecordingStorage("new/receipt.jpg");
        RecordingExpenseService expenses = new RecordingExpenseService();

        ReceiptWorkflow.Result result = ReceiptWorkflow.finish(
                expenses, storage, existing, existing, Path.of("receipt.jpg"), false
        );

        assertFalse(result.warning());
        assertEquals(List.of("old/receipt.jpg"), storage.deletedPaths);
        assertEquals("new/receipt.jpg", expenses.lastUpdated.receiptPath());
    }

    @Test
    void removesReceiptAndPersistsNoReceiptMetadata() {
        Expense existing = expense("old/receipt.jpg", ReceiptStatus.AVAILABLE);
        RecordingStorage storage = new RecordingStorage(null);
        RecordingExpenseService expenses = new RecordingExpenseService();

        ReceiptWorkflow.Result result = ReceiptWorkflow.finish(
                expenses, storage, existing, existing, null, true
        );

        assertFalse(result.warning());
        assertEquals(ReceiptStatus.NO_RECEIPT, expenses.lastUpdated.receiptStatus());
        assertEquals(null, expenses.lastUpdated.receiptPath());
        assertEquals(List.of("old/receipt.jpg"), storage.deletedPaths);
    }

    @Test
    void uploadFailureLeavesExpenseSavedAndMarksUploadFailed() {
        Expense saved = expense(null, ReceiptStatus.NO_RECEIPT);
        RecordingStorage storage = new RecordingStorage(null);
        storage.uploadFailure = new ServiceException("storage unavailable");
        RecordingExpenseService expenses = new RecordingExpenseService();

        ReceiptWorkflow.Result result = ReceiptWorkflow.finish(
                expenses, storage, saved, null, Path.of("receipt.jpg"), false
        );

        assertTrue(result.warning());
        assertEquals(saved, result.expense());
        assertEquals(ReceiptStatus.UPLOAD_FAILED, expenses.lastUpdated.receiptStatus());
        assertEquals(List.of(), storage.deletedPaths);
        assertTrue(result.message().contains("enter the details manually"));
    }

    @Test
    void failedMetadataUpdateCleansUpNewUpload() {
        Expense saved = expense(null, ReceiptStatus.NO_RECEIPT);
        RecordingStorage storage = new RecordingStorage("new/receipt.jpg");
        RecordingExpenseService expenses = new RecordingExpenseService();
        expenses.updateFailure = new ServiceException("expense update failed");

        ReceiptWorkflow.Result result = ReceiptWorkflow.finish(
                expenses, storage, saved, null, Path.of("receipt.jpg"), false
        );

        assertTrue(result.warning());
        assertEquals(List.of("new/receipt.jpg"), storage.deletedPaths);
        assertEquals(saved, result.expense());
    }

    private Expense expense(String receiptPath, ReceiptStatus status) {
        return new Expense(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                "Coffee",
                BigDecimal.TEN,
                LocalDate.of(2026, 9, 7),
                PaymentMethod.CASH,
                null,
                receiptPath,
                status,
                null,
                null
        );
    }

    private static final class RecordingStorage implements StorageService {
        private final String uploadedPath;
        private final List<String> deletedPaths = new ArrayList<>();
        private ServiceException uploadFailure;

        private RecordingStorage(String uploadedPath) {
            this.uploadedPath = uploadedPath;
        }

        @Override
        public String uploadReceipt(UUID expenseId, Path localFile) {
            if (uploadFailure != null) {
                throw uploadFailure;
            }
            return uploadedPath;
        }

        @Override
        public byte[] downloadReceipt(String receiptPath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteReceipt(String receiptPath) {
            deletedPaths.add(receiptPath);
        }
    }

    private static final class RecordingExpenseService implements ExpenseService {
        private Expense lastUpdated;
        private ServiceException updateFailure;

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
            if (updateFailure != null) {
                throw updateFailure;
            }
            lastUpdated = expense;
            return expense;
        }

        @Override
        public void deleteExpense(UUID expenseId) {
            throw new UnsupportedOperationException();
        }
    }
}