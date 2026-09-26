package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataServiceTest {

    @Test
    void testGetDefaultCategories() {
        List<Category> categories = DemoDataService.getDefaultCategories();
        assertFalse(categories.isEmpty());
        assertTrue(categories.stream().anyMatch(c -> c.name().contains("Food")));
        assertTrue(categories.stream().anyMatch(c -> c.name().contains("Travel")));
        assertTrue(categories.stream().anyMatch(c -> c.name().contains("Shopping")));
    }

    @Test
    void testGetSeedExpenses() {
        List<Expense> seed = DemoDataService.getSeedExpenses();
        assertFalse(seed.isEmpty());
        assertTrue(seed.size() >= 5);
        for (Expense expense : seed) {
            assertEquals(DemoDataService.DEMO_USER_ID, expense.userId());
            assertNotNull(expense.merchant());
            assertTrue(expense.amount().signum() > 0);
        }
    }

    @Test
    void testInMemoryExpenseRepositoryCrud() {
        DemoDataService.InMemoryExpenseRepository repo = new DemoDataService.InMemoryExpenseRepository();
        List<Expense> initial = repo.findByUser(DemoDataService.DEMO_USER_ID);
        int initialCount = initial.size();

        UUID newId = UUID.randomUUID();
        Expense newExp = new Expense(
                newId, DemoDataService.DEMO_USER_ID, DemoDataService.CAT_FOOD,
                "New Test Merchant", new BigDecimal("15.50"),
                LocalDate.now(), PaymentMethod.CASH,
                "Testing CRUD", null, ReceiptStatus.NO_RECEIPT,
                Instant.now(), Instant.now()
        );

        repo.create(newExp);
        assertEquals(initialCount + 1, repo.findByUser(DemoDataService.DEMO_USER_ID).size());
        assertTrue(repo.findById(newId).isPresent());

        // Update
        Expense updated = new Expense(
                newId, DemoDataService.DEMO_USER_ID, DemoDataService.CAT_FOOD,
                "Updated Merchant Name", new BigDecimal("20.00"),
                LocalDate.now(), PaymentMethod.CASH,
                "Testing update", null, ReceiptStatus.NO_RECEIPT,
                Instant.now(), Instant.now()
        );
        repo.update(updated);
        assertEquals("Updated Merchant Name", repo.findById(newId).get().merchant());
        assertEquals(new BigDecimal("20.00"), repo.findById(newId).get().amount());

        // Delete
        repo.delete(newId);
        assertFalse(repo.findById(newId).isPresent());
        assertEquals(initialCount, repo.findByUser(DemoDataService.DEMO_USER_ID).size());
    }
}
