package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetServiceTest {
    private static final UUID USER = UUID.randomUUID();
    private static final Category FOOD = new Category(UUID.randomUUID(), "Food");
    private static final Category TRAVEL = new Category(UUID.randomUUID(), "Travel");
    private static final YearMonth SEPT = YearMonth.of(2026, 9);

    private static Expense expense(Category category, String amount, LocalDate date) {
        return new Expense(UUID.randomUUID(), USER, category.id(), "Shop", new BigDecimal(amount), date,
                PaymentMethod.CASH, null, null, ReceiptStatus.NO_RECEIPT, Instant.now(), Instant.now());
    }

    @Test
    void evaluatesOverallAndCategoryBudgetsForTheMonthOnly() {
        List<Budget> budgets = List.of(
                new Budget(UUID.randomUUID(), null, new BigDecimal("1000")),
                new Budget(UUID.randomUUID(), FOOD.id(), new BigDecimal("400")));
        List<Expense> expenses = List.of(
                expense(FOOD, "300", LocalDate.of(2026, 9, 5)),
                expense(TRAVEL, "100", LocalDate.of(2026, 9, 6)),
                expense(FOOD, "999", LocalDate.of(2026, 8, 30)));

        List<BudgetService.Status> statuses =
                BudgetService.evaluate(budgets, expenses, List.of(FOOD, TRAVEL), SEPT, 80);

        assertEquals(2, statuses.size());
        BudgetService.Status overall = statuses.get(0);
        assertEquals("Overall", overall.name());
        assertEquals(0, new BigDecimal("400").compareTo(overall.spent()));
        assertEquals(BudgetService.Level.OK, overall.level());

        BudgetService.Status food = statuses.get(1);
        assertEquals("Food", food.name());
        assertEquals(75.0, food.percent());
        assertEquals(BudgetService.Level.OK, food.level());
    }

    @Test
    void levelsFollowTheAlertThreshold() {
        Budget budget = new Budget(UUID.randomUUID(), FOOD.id(), new BigDecimal("100"));
        List<Category> categories = List.of(FOOD);

        assertEquals(BudgetService.Level.OK, level(budget, categories, "79", 80));
        assertEquals(BudgetService.Level.WARNING, level(budget, categories, "80", 80));
        assertEquals(BudgetService.Level.WARNING, level(budget, categories, "99.9", 80));
        assertEquals(BudgetService.Level.EXCEEDED, level(budget, categories, "100", 80));
        assertEquals(BudgetService.Level.EXCEEDED, level(budget, categories, "250", 80));
    }

    @Test
    void alertsOnlyIncludeTheExpensesCategoryAndTheOverallBudget() {
        List<Budget> budgets = List.of(
                new Budget(UUID.randomUUID(), null, new BigDecimal("100")),
                new Budget(UUID.randomUUID(), FOOD.id(), new BigDecimal("100")),
                new Budget(UUID.randomUUID(), TRAVEL.id(), new BigDecimal("100")));
        List<Expense> expenses = List.of(
                expense(FOOD, "90", LocalDate.of(2026, 9, 1)),
                expense(TRAVEL, "90", LocalDate.of(2026, 9, 2)));

        List<BudgetService.Status> statuses =
                BudgetService.evaluate(budgets, expenses, List.of(FOOD, TRAVEL), SEPT, 80);
        List<BudgetService.Status> alerts = BudgetService.alertsFor(statuses, FOOD.id());

        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().anyMatch(a -> a.categoryId() == null));
        assertTrue(alerts.stream().anyMatch(a -> FOOD.id().equals(a.categoryId())));
    }

    private static BudgetService.Level level(Budget budget, List<Category> categories, String spent, int threshold) {
        List<Expense> expenses = List.of(expense(FOOD, spent, LocalDate.of(2026, 9, 10)));
        return BudgetService.evaluate(List.of(budget), expenses, categories, SEPT, threshold).get(0).level();
    }
}
