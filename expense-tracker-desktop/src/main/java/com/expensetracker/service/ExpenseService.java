package com.expensetracker.service;

import com.expensetracker.model.Expense;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public interface ExpenseService {
    Expense createExpense(Expense expense);

    List<Expense> getExpenses(UUID userId);

    Expense updateExpense(Expense expense);

    void deleteExpense(UUID expenseId);

    default List<Expense> searchExpenses(UUID userId, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return getExpenses(userId).stream()
                .filter(expense -> normalizedQuery.isBlank()
                        || expense.merchant().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || (expense.description() != null
                        && expense.description().toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .toList();
    }
}