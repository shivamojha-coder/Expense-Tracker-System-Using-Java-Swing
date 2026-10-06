package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.ExpenseRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DemoDataService {
    public static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String DEMO_USER_EMAIL = "demo.user@expensetracker.local";

    public static final UUID CAT_FOOD = UUID.fromString("11111111-1111-1111-1111-111111110001");
    public static final UUID CAT_TRAVEL = UUID.fromString("11111111-1111-1111-1111-111111110002");
    public static final UUID CAT_SHOPPING = UUID.fromString("11111111-1111-1111-1111-111111110003");
    public static final UUID CAT_BILLS = UUID.fromString("11111111-1111-1111-1111-111111110004");
    public static final UUID CAT_ENTERTAINMENT = UUID.fromString("11111111-1111-1111-1111-111111110005");
    public static final UUID CAT_HEALTHCARE = UUID.fromString("11111111-1111-1111-1111-111111110006");
    public static final UUID CAT_OTHER = UUID.fromString("11111111-1111-1111-1111-111111110007");

    private static final List<Category> DEFAULT_CATEGORIES = List.of(
            new Category(CAT_FOOD, "Food & Dining"),
            new Category(CAT_TRAVEL, "Transportation & Travel"),
            new Category(CAT_SHOPPING, "Shopping"),
            new Category(CAT_BILLS, "Bills & Utilities"),
            new Category(CAT_ENTERTAINMENT, "Entertainment"),
            new Category(CAT_HEALTHCARE, "Healthcare"),
            new Category(CAT_OTHER, "Other")
    );

    public static List<Category> getDefaultCategories() {
        return new ArrayList<>(DEFAULT_CATEGORIES);
    }

    public static List<Expense> getSeedExpenses() {
        LocalDate today = LocalDate.now();
        List<Expense> expenses = new ArrayList<>();

        expenses.add(new Expense(
                UUID.randomUUID(), DEMO_USER_ID, CAT_FOOD,
                "Starbucks Coffee", new BigDecimal("4.85"),
                today.minusDays(1), PaymentMethod.CREDIT_CARD,
                "Morning latte and pastry", null,
                ReceiptStatus.NO_RECEIPT, Instant.now(), Instant.now()
        ));

        expenses.add(new Expense(
                UUID.randomUUID(), DEMO_USER_ID, CAT_SHOPPING,
                "Amazon Online Store", new BigDecimal("79.99"),
                today.minusDays(3), PaymentMethod.DEBIT_CARD,
                "Wireless mechanical keyboard", "receipts/amazon-keyboard.png",
                ReceiptStatus.AVAILABLE, Instant.now(), Instant.now()
        ));

        expenses.add(new Expense(
                UUID.randomUUID(), DEMO_USER_ID, CAT_FOOD,
                "Trader Joe's", new BigDecimal("54.30"),
                today.minusDays(5), PaymentMethod.UPI,
                "Weekly fresh grocery shopping", null,
                ReceiptStatus.NO_RECEIPT, Instant.now(), Instant.now()
        ));

        expenses.add(new Expense(
                UUID.randomUUID(), DEMO_USER_ID, CAT_TRAVEL,
                "Metro Monthly Transit Pass", new BigDecimal("65.00"),
                today.withDayOfMonth(1), PaymentMethod.CREDIT_CARD,
                "Commuter pass for subway & bus", null,
                ReceiptStatus.NO_RECEIPT, Instant.now(), Instant.now()
        ));

        expenses.add(new Expense(
                UUID.randomUUID(), DEMO_USER_ID, CAT_BILLS,
                "Fiber Internet Utility", new BigDecimal("59.99"),
                today.minusDays(10), PaymentMethod.BANK_TRANSFER,
                "Monthly broadband billing", null,
                ReceiptStatus.NO_RECEIPT, Instant.now(), Instant.now()
        ));

        expenses.add(new Expense(
                UUID.randomUUID(), DEMO_USER_ID, CAT_ENTERTAINMENT,
                "Cinemark Theaters", new BigDecimal("28.50"),
                today.minusDays(12), PaymentMethod.CASH,
                "Movie tickets with snacks", null,
                ReceiptStatus.NO_RECEIPT, Instant.now(), Instant.now()
        ));

        expenses.add(new Expense(
                UUID.randomUUID(), DEMO_USER_ID, CAT_HEALTHCARE,
                "CVS Pharmacy", new BigDecimal("22.40"),
                today.minusDays(15), PaymentMethod.DEBIT_CARD,
                "Prescriptions and vitamins", null,
                ReceiptStatus.NO_RECEIPT, Instant.now(), Instant.now()
        ));

        return expenses;
    }

    public static final class InMemoryCategoryRepository implements CategoryRepository {
        private final List<Category> categories = new CopyOnWriteArrayList<>(getDefaultCategories());

        @Override
        public List<Category> findAll() {
            return new ArrayList<>(categories);
        }
    }

    public static final class InMemoryExpenseRepository implements ExpenseRepository {
        private final List<Expense> expenses = new CopyOnWriteArrayList<>(getSeedExpenses());

        @Override
        public Expense create(Expense expense) {
            expenses.add(0, expense);
            return expense;
        }

        @Override
        public Optional<Expense> findById(UUID expenseId) {
            return expenses.stream().filter(e -> e.id().equals(expenseId)).findFirst();
        }

        @Override
        public List<Expense> findByUser(UUID userId) {
            return expenses.stream()
                    .filter(e -> e.userId().equals(userId))
                    .toList();
        }

        @Override
        public Expense update(Expense expense) {
            for (int i = 0; i < expenses.size(); i++) {
                if (expenses.get(i).id().equals(expense.id())) {
                    expenses.set(i, expense);
                    return expense;
                }
            }
            expenses.add(expense);
            return expense;
        }

        @Override
        public void delete(UUID expenseId) {
            expenses.removeIf(e -> e.id().equals(expenseId));
        }
    }
}
