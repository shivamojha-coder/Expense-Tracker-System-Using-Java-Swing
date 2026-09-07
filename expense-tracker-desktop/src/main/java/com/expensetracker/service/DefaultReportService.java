package com.expensetracker.service;

import com.expensetracker.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class DefaultReportService implements ReportService {
    @Override
    public ReportSummary generateReport(List<Expense> expenses, LocalDate from, LocalDate to) {
        Objects.requireNonNull(expenses, "Expenses are required.");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Report start date must not be after its end date.");
        }

        List<Expense> filteredExpenses = expenses.stream()
                .filter(expense -> from == null || !expense.expenseDate().isBefore(from))
                .filter(expense -> to == null || !expense.expenseDate().isAfter(to))
                .toList();
        BigDecimal total = filteredExpenses.stream()
                .map(Expense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportSummary(from, to, filteredExpenses, total, filteredExpenses.size());
    }
}