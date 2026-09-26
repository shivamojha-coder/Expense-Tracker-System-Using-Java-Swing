package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface ReportService {
    ReportSummary generateReport(List<Expense> expenses, LocalDate from, LocalDate to);

    DetailedReport generateDetailedReport(
            List<Expense> expenses,
            List<Category> categories,
            LocalDate from,
            LocalDate to
    );

    void exportToCsv(DetailedReport report, List<Category> categories, Path destination) throws IOException;

    void exportToPdf(DetailedReport report, List<Category> categories, Path destination, String userEmail) throws IOException;

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
            BigDecimal total,
            int count
    ) {
    }

    record CategoryBreakdown(
            UUID categoryId,
            String categoryName,
            int count,
            BigDecimal totalAmount,
            double percentage
    ) {
    }

    record PaymentBreakdown(
            PaymentMethod paymentMethod,
            int count,
            BigDecimal totalAmount,
            double percentage
    ) {
    }

    record DetailedReport(
            LocalDate from,
            LocalDate to,
            List<Expense> expenses,
            BigDecimal totalAmount,
            int count,
            BigDecimal averageAmount,
            Expense highestExpense,
            List<CategoryBreakdown> categoryBreakdowns,
            List<PaymentBreakdown> paymentBreakdowns
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