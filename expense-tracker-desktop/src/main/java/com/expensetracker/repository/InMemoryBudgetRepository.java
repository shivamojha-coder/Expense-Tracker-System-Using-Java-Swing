package com.expensetracker.repository;

import com.expensetracker.model.Budget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Budgets kept in memory for offline demo mode. */
public final class InMemoryBudgetRepository implements BudgetRepository {
    private final List<Budget> budgets = new ArrayList<>();

    @Override
    public synchronized List<Budget> findAll() {
        return List.copyOf(budgets);
    }

    @Override
    public synchronized Budget save(UUID categoryId, BigDecimal monthlyLimit) {
        Budget existing = budgets.stream()
                .filter(budget -> Objects.equals(budget.categoryId(), categoryId))
                .findFirst()
                .orElse(null);
        Budget saved = new Budget(existing == null ? UUID.randomUUID() : existing.id(), categoryId, monthlyLimit);
        if (existing != null) {
            budgets.remove(existing);
        }
        budgets.add(saved);
        return saved;
    }

    @Override
    public synchronized void delete(UUID budgetId) {
        budgets.removeIf(budget -> budget.id().equals(budgetId));
    }
}
