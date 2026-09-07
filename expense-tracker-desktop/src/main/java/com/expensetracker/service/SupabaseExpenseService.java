package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SupabaseExpenseService implements ExpenseService {
    private final ExpenseRepository repository;
    private final UUID currentUserId;

    public SupabaseExpenseService(ExpenseRepository repository, UUID currentUserId) {
        this.repository = repository;
        this.currentUserId = currentUserId;
    }

    @Override
    public Expense createExpense(Expense expense) {
        requireOwned(expense);
        Expense created = repository.create(expense);
        requireOwned(created);
        return created;
    }

    @Override
    public List<Expense> getExpenses(UUID userId) {
        requireCurrentUser(userId);
        return repository.findByUser(currentUserId).stream()
                .peek(this::requireOwned)
                .toList();
    }

    @Override
    public Expense updateExpense(Expense expense) {
        requireOwned(expense);
        if (expense.id() == null) {
            throw new ServiceException("Expense identifier is required.");
        }
        Expense updated = repository.update(expense);
        requireOwned(updated);
        return updated;
    }

    @Override
    public void deleteExpense(UUID expenseId) {
        if (expenseId == null) {
            throw new ServiceException("Expense identifier is required.");
        }
        Expense expense = repository.findById(expenseId)
                .orElseThrow(() -> new ServiceException("The expense was not found."));
        requireOwned(expense);
        repository.delete(expenseId);
    }

    @Override
    public List<Expense> searchExpenses(UUID userId, String query) {
        requireCurrentUser(userId);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return getExpenses(currentUserId).stream()
                .filter(expense -> normalizedQuery.isBlank()
                        || expense.merchant().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || (expense.description() != null
                        && expense.description().toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .toList();
    }

    private void requireOwned(Expense expense) {
        if (expense == null || !currentUserId.equals(expense.userId())) {
            throw new ServiceException("You can only change your own expenses.");
        }
    }

    private void requireCurrentUser(UUID userId) {
        if (!currentUserId.equals(userId)) {
            throw new ServiceException("You can only view your own expenses.");
        }
    }
}