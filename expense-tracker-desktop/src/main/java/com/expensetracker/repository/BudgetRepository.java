package com.expensetracker.repository;

import com.expensetracker.model.Budget;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BudgetRepository {
    List<Budget> findAll();

    /** Creates or updates the limit for a category (null category = overall monthly budget). */
    Budget save(UUID categoryId, BigDecimal monthlyLimit);

    void delete(UUID budgetId);
}
