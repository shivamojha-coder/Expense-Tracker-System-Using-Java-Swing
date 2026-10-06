package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.StorageService;

import java.util.List;
import java.util.UUID;

/**
 * Applies a delete to the UI's current expense snapshot without removing a row
 * until the service confirms that the delete succeeded. The receipt file, if any,
 * is removed afterwards so deleted expenses do not leave orphaned files in storage.
 */
final class ExpenseDeleteWorkflow {
    private ExpenseDeleteWorkflow() {
    }

    static DeleteResult delete(
            ExpenseService expenseService,
            StorageService storageService,
            List<Expense> currentExpenses,
            UUID expenseId
    ) {
        String receiptPath = currentExpenses.stream()
                .filter(expense -> expense.id().equals(expenseId))
                .findFirst()
                .map(Expense::receiptPath)
                .orElse(null);
        try {
            expenseService.deleteExpense(expenseId);
        } catch (RuntimeException error) {
            return new DeleteResult(false, List.copyOf(currentExpenses), error, false);
        }
        boolean receiptRemoved = true;
        if (receiptPath != null && !receiptPath.isBlank()) {
            try {
                storageService.deleteReceipt(receiptPath);
            } catch (RuntimeException error) {
                receiptRemoved = false;
            }
        }
        List<Expense> remaining = currentExpenses.stream()
                .filter(expense -> !expense.id().equals(expenseId))
                .toList();
        return new DeleteResult(true, remaining, null, !receiptRemoved);
    }

    /** @param receiptLeftBehind true when the expense is gone but its receipt file could not be removed */
    record DeleteResult(boolean deleted, List<Expense> expenses, RuntimeException error, boolean receiptLeftBehind) {
    }
}
