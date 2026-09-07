package com.expensetracker.service;

import com.expensetracker.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public interface ReportService {
    ReportSummary generateReport(List<Expense> expenses, LocalDate from, LocalDate to);

    default DashboardSummary generateDashboard(List<Expense> expenses, LocalDate currentDate) {
        Objects.requireNonNull(expenses, "Expenses are required.");
        Objects.requireNonNull(currentDate, "Current date is required.");

        ReportSummary allExpenses = generateReport(expenses, null, null);
        LocalDate monthStart = currentDate.withDayOfMonth(1);
        LocalDate monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
        ReportSummary currentMonth = generateReport(expenses, monthStart, monthEnd);
        Expense highestExpense = allExpenses.expenses().stream()
                .max(Comparator.comparing(Expense::amount))
                .orElse(null);
        List<Expense> recentExpenses = allExpenses.expenses().stream()
                .sorted(Comparator.comparing(Expense::expenseDate)
                        .reversed()
                        .thenComparing(
                                Expense::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .limit(5)
                .toList();

        return new DashboardSummary(
                allExpenses.total(),
                currentMonth.total(),
                allExpenses.count(),
                highestExpense,
                recentExpenses
        );
    }

    record ReportSummary(
            LocalDate from,
            LocalDate to,
            List<Expense> expenses,
            java.math.BigDecimal total,
            int count
    ) {
    }

    record DashboardSummary(
            BigDecimal total,
            BigDecimal currentMonthTotal,
            int count,
            Expense highestExpense,
            List<Expense> recentExpenses
    ) {
    }
}