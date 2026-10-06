package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Compares monthly spending with the user's budgets. */
public final class BudgetService {
    private BudgetService() {
    }

    public enum Level { OK, WARNING, EXCEEDED }

    /**
     * @param categoryId null for the overall monthly budget
     * @param percent    spent / limit * 100 (may exceed 100)
     */
    public record Status(UUID categoryId, String name, BigDecimal limit, BigDecimal spent, double percent, Level level) {
    }

    public static List<Status> evaluate(
            List<Budget> budgets,
            List<Expense> expenses,
            List<Category> categories,
            YearMonth month,
            int thresholdPercent
    ) {
        Map<UUID, String> names = new HashMap<>();
        for (Category category : categories) {
            names.put(category.id(), category.name());
        }
        BigDecimal overallSpent = BigDecimal.ZERO;
        Map<UUID, BigDecimal> perCategory = new HashMap<>();
        for (Expense expense : expenses) {
            if (!YearMonth.from(expense.expenseDate()).equals(month)) {
                continue;
            }
            overallSpent = overallSpent.add(expense.amount());
            perCategory.merge(expense.categoryId(), expense.amount(), BigDecimal::add);
        }

        List<Status> result = new ArrayList<>();
        for (Budget budget : budgets) {
            BigDecimal spent = budget.categoryId() == null
                    ? overallSpent
                    : perCategory.getOrDefault(budget.categoryId(), BigDecimal.ZERO);
            String name = budget.categoryId() == null
                    ? "Overall"
                    : names.getOrDefault(budget.categoryId(), "Unknown category");
            result.add(status(budget, name, spent, thresholdPercent));
        }
        result.sort(Comparator
                .comparing((Status status) -> status.categoryId() != null)
                .thenComparing(Status::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /** Statuses that need the user's attention for a given expense: its category's budget and the overall one. */
    public static List<Status> alertsFor(List<Status> statuses, UUID expenseCategoryId) {
        return statuses.stream()
                .filter(status -> status.level() != Level.OK)
                .filter(status -> status.categoryId() == null || Objects.equals(status.categoryId(), expenseCategoryId))
                .toList();
    }

    private static Status status(Budget budget, String name, BigDecimal spent, int thresholdPercent) {
        double percent = budget.monthlyLimit().signum() <= 0
                ? 0
                : spent.multiply(BigDecimal.valueOf(100))
                .divide(budget.monthlyLimit(), 1, RoundingMode.HALF_UP).doubleValue();
        Level level = percent >= 100 ? Level.EXCEEDED : percent >= thresholdPercent ? Level.WARNING : Level.OK;
        return new Status(budget.categoryId(), name, budget.monthlyLimit(), spent, percent, level);
    }
}
