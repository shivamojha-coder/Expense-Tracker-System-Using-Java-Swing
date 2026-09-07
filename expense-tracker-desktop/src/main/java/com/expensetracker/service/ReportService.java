package com.expensetracker.service;

import com.expensetracker.model.Expense;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ReportSummary generateReport(List<Expense> expenses, LocalDate from, LocalDate to);

    record ReportSummary(
            LocalDate from,
            LocalDate to,
            List<Expense> expenses,
            java.math.BigDecimal total,
            int count
    ) {
    }
}