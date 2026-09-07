package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ServiceException;
import com.expensetracker.service.StorageService;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Coordinates expense metadata and receipt storage changes after an expense is
 * saved. Keeping this boundary outside the Swing frame makes the failure and
 * cleanup behavior testable without a display server.
 */
final class ReceiptWorkflow {
    private ReceiptWorkflow() {
    }

    static Result finish(
            ExpenseService expenseService,
            StorageService storageService,
            Expense savedExpense,
            Expense existing,
            Path localFile,
            boolean removeExisting
    ) {
        String savedMessage = existing == null
                ? "Expense added successfully."
                : "Expense updated successfully.";
        String existingPath = existing == null ? null : existing.receiptPath();

        if (localFile != null) {
            String uploadedPath = null;
            try {
                uploadedPath = storageService.uploadReceipt(savedExpense.id(), localFile);
                Expense attached = withReceipt(savedExpense, uploadedPath, ReceiptStatus.AVAILABLE);
                Expense persisted = expenseService.updateExpense(attached);
                if (existingPath != null && !existingPath.equals(uploadedPath)) {
                    try {
                        storageService.deleteReceipt(existingPath);
                    } catch (ServiceException ignored) {
                        return new Result(
                                persisted,
                                savedMessage + " The new receipt is attached, but the old receipt could not be removed.",
                                true
                        );
                    }
                }
                return new Result(persisted, savedMessage, false);
            } catch (ServiceException exception) {
                if (uploadedPath != null) {
                    try {
                        storageService.deleteReceipt(uploadedPath);
                    } catch (ServiceException ignored) {
                        // Do not hide the original failure from the user.
                    }
                }
                if (existingPath == null) {
                    try {
                        expenseService.updateExpense(
                                withReceipt(savedExpense, null, ReceiptStatus.UPLOAD_FAILED)
                        );
                    } catch (ServiceException ignored) {
                        // The manual expense is still saved even if the status update fails.
                    }
                }
                return new Result(
                        savedExpense,
                        savedMessage + " The receipt could not be uploaded: "
                                + exception.getMessage()
                                + " You can enter the details manually and try again.",
                        true
                );
            }
        }

        if (removeExisting && existingPath != null) {
            try {
                Expense withoutReceipt = withReceipt(savedExpense, null, ReceiptStatus.NO_RECEIPT);
                Expense persisted = expenseService.updateExpense(withoutReceipt);
                storageService.deleteReceipt(existingPath);
                return new Result(persisted, savedMessage, false);
            } catch (ServiceException exception) {
                return new Result(
                        savedExpense,
                        savedMessage + " The receipt could not be removed: " + exception.getMessage(),
                        true
                );
            }
        }
        return new Result(savedExpense, savedMessage, false);
    }

    private static Expense withReceipt(Expense expense, String receiptPath, ReceiptStatus status) {
        return new Expense(
                expense.id(),
                expense.userId(),
                expense.categoryId(),
                expense.merchant(),
                expense.amount(),
                expense.expenseDate(),
                expense.paymentMethod(),
                expense.description(),
                receiptPath,
                status,
                expense.createdAt(),
                Instant.now()
        );
    }

    record Result(Expense expense, String message, boolean warning) {
    }
}