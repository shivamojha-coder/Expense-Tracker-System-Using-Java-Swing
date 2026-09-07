package com.expensetracker.service;

import com.expensetracker.model.Expense;

import java.util.List;
import java.util.UUID;

public interface ExpenseService {
    Expense createExpense(Expense expense);

    List<Expense> getExpenses(UUID userId);

    Expense updateExpense(Expense expense);

    void deleteExpense(UUID expenseId);
}