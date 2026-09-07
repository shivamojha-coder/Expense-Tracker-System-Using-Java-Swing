package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupabaseExpenseServiceTest {
    private final UUID currentUserId = UUID.randomUUID();
    private final UUID anotherUserId = UUID.randomUUID();

    @Test
    void rejectsCreatingAnotherUsersExpense() {
        RecordingRepository repository = new RecordingRepository();
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        assertThrows(ServiceException.class, () -> service.createExpense(expense(anotherUserId)));
        assertEquals(0, repository.createCalls);
    }

    @Test
    void rejectsForeignExpenseReturnedFromCreate() {
        RecordingRepository repository = new RecordingRepository();
        repository.createdExpense = expense(anotherUserId);
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        assertThrows(ServiceException.class, () -> service.createExpense(expense(currentUserId)));
    }

    @Test
    void rejectsReadingAnotherUsersExpenses() {
        RecordingRepository repository = new RecordingRepository();
        repository.expenses = List.of(expense(anotherUserId));
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        assertThrows(ServiceException.class, () -> service.getExpenses(anotherUserId));
        assertEquals(0, repository.findByUserCalls);
    }

    @Test
    void rejectsForeignExpenseReturnedFromRead() {
        RecordingRepository repository = new RecordingRepository();
        repository.expenses = List.of(expense(anotherUserId));
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        assertThrows(ServiceException.class, () -> service.getExpenses(currentUserId));
    }

    @Test
    void rejectsUpdatingAnotherUsersExpense() {
        RecordingRepository repository = new RecordingRepository();
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        assertThrows(ServiceException.class, () -> service.updateExpense(expense(anotherUserId)));
        assertEquals(0, repository.updateCalls);
    }

    @Test
    void rejectsForeignExpenseReturnedFromUpdate() {
        RecordingRepository repository = new RecordingRepository();
        repository.updatedExpense = expense(anotherUserId);
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        assertThrows(ServiceException.class, () -> service.updateExpense(expense(currentUserId)));
    }

    @Test
    void rejectsDeletingAnotherUsersExpenseEvenIfRepositoryReturnsIt() {
        RecordingRepository repository = new RecordingRepository();
        repository.expenseById = Optional.of(expense(anotherUserId));
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        assertThrows(ServiceException.class, () -> service.deleteExpense(repository.expenseById.get().id()));
        assertEquals(0, repository.deleteCalls);
    }

    @Test
    void deletesOwnedExpenseAfterOwnershipCheck() {
        RecordingRepository repository = new RecordingRepository();
        repository.expenseById = Optional.of(expense(currentUserId));
        SupabaseExpenseService service = new SupabaseExpenseService(repository, currentUserId);

        service.deleteExpense(repository.expenseById.get().id());

        assertEquals(1, repository.deleteCalls);
    }

    private Expense expense(UUID userId) {
        return new Expense(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                "Coffee",
                BigDecimal.TEN,
                LocalDate.of(2026, 9, 7),
                PaymentMethod.CASH,
                null,
                null,
                ReceiptStatus.NO_RECEIPT,
                null,
                null
        );
    }

    private static final class RecordingRepository implements ExpenseRepository {
        private int createCalls;
        private int findByUserCalls;
        private int updateCalls;
        private int deleteCalls;
        private Optional<Expense> expenseById = Optional.empty();
        private List<Expense> expenses = List.of();
        private Expense createdExpense;
        private Expense updatedExpense;

        @Override
        public Expense create(Expense expense) {
            createCalls++;
            return createdExpense == null ? expense : createdExpense;
        }

        @Override
        public Optional<Expense> findById(UUID expenseId) {
            return expenseById;
        }

        @Override
        public List<Expense> findByUser(UUID userId) {
            findByUserCalls++;
            return expenses;
        }

        @Override
        public Expense update(Expense expense) {
            updateCalls++;
            return updatedExpense == null ? expense : updatedExpense;
        }

        @Override
        public void delete(UUID expenseId) {
            deleteCalls++;
        }
    }
}