package com.expensetracker.repository;

import com.expensetracker.model.Expense;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository {
    Expense create(Expense expense);

    Optional<Expense> findById(UUID expenseId);

    List<Expense> findByUser(UUID userId);

    Expense update(Expense expense);

    void delete(UUID expenseId);
}