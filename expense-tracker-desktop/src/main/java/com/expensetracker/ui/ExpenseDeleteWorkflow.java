package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseService;

import java.util.List;
import java.util.UUID;

/**
 * Applies a delete to the UI's current expense snapshot without removing a row
 * until the service confirms that the delete succeeded.
 */
final class ExpenseDeleteWorkflow {
    private ExpenseDeleteWorkflow() {
    }

    static DeleteResult delete(
            ExpenseService expenseService,
            List<Expense> currentExpenses,
            UUID expenseId
    ) {
        try {
            expenseService.deleteExpense(expenseId);
            List<Expense> remaining = currentExpenses.stream()
                    .filter(expense -> !expense.id().equals(expenseId))
                    .toList();
            return new DeleteResult(true, remaining, null);
        } catch (RuntimeException error) {
            return new DeleteResult(false, List.copyOf(currentExpenses), error);
        }
    }

    record DeleteResult(boolean deleted, List<Expense> expenses, RuntimeException error) {
    }
}