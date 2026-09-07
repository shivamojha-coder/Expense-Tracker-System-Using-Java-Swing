package com.expensetracker.repository;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.ServiceException;
import com.expensetracker.supabase.SupabaseClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupabaseExpenseRepositoryTest {
    private final UUID currentUserId = UUID.randomUUID();
    private final UUID anotherUserId = UUID.randomUUID();
    private HttpServer server;
    private AtomicReference<String> requestPath;
    private AtomicReference<String> requestBody;
    private SupabaseExpenseRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        requestPath = new AtomicReference<>();
        requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/rest/v1/expenses", this::handleRequest);
        server.start();
        AppConfig config = new AppConfig(
                "http://localhost:" + server.getAddress().getPort(),
                "test-anon-key",
                "expense-receipts"
        );
        repository = new SupabaseExpenseRepository(
                new SupabaseClient(config),
                "test-access-token",
                currentUserId
        );
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void findByIdScopesLookupToCurrentUser() {
        UUID expenseId = UUID.randomUUID();

        assertTrue(repository.findById(expenseId).isEmpty());

        assertEquals(
                "select=*&user_id=eq." + currentUserId
                        + "&id=eq." + expenseId
                        + "&order=expense_date.desc,created_at.desc",
                requestPath.get()
        );
    }

    @Test
    void findByIdRejectsForeignRecordReturnedByBackend() {
        Expense foreignExpense = expense(anotherUserId);
        server.removeContext("/rest/v1/expenses");
        server.createContext("/rest/v1/expenses", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, expenseJson(foreignExpense));
        });

        assertThrows(ServiceException.class, () -> repository.findById(foreignExpense.id()));
    }

    @Test
    void findByUserRejectsAnotherUserBeforeRequest() {
        assertThrows(ServiceException.class, () -> repository.findByUser(anotherUserId));
        assertEquals(null, requestPath.get());
    }

    @Test
    void updateRejectsAnotherUsersExpenseBeforeRequest() {
        assertThrows(ServiceException.class, () -> repository.update(expense(anotherUserId)));
        assertEquals(null, requestPath.get());
    }

    @Test
    void createRejectsAnotherUsersExpenseBeforeRequest() {
        assertThrows(ServiceException.class, () -> repository.create(expense(anotherUserId)));
        assertEquals(null, requestPath.get());
    }

    @Test
    void deleteScopesMutationToCurrentUserAndReportsNoDeletedRow() {
        UUID expenseId = UUID.randomUUID();

        assertThrows(ServiceException.class, () -> repository.delete(expenseId));

        assertEquals(
                "select=*&user_id=eq." + currentUserId
                        + "&id=eq." + expenseId
                        + "&order=expense_date.desc,created_at.desc",
                requestPath.get()
        );
    }

    @Test
    void createSendsCurrentUserInPayload() {
        Expense expense = expense(currentUserId);
        requestBody.set(null);

        server.removeContext("/rest/v1/expenses");
        server.createContext("/rest/v1/expenses", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawQuery());
            requestBody.set(readBody(exchange));
            respond(exchange, 200, expenseJson(expense));
        });

        repository.create(expense);

        assertTrue(requestBody.get().contains("\"user_id\":\"" + currentUserId + "\""));
    }

    @Test
    void updateRejectsForeignRecordReturnedByBackend() {
        Expense foreignExpense = expense(anotherUserId);
        server.removeContext("/rest/v1/expenses");
        server.createContext("/rest/v1/expenses", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, expenseJson(foreignExpense));
        });

        assertThrows(ServiceException.class, () -> repository.update(expense(currentUserId)));
    }

    private Expense expense(UUID userId) {
        return new Expense(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                "Coffee",
                java.math.BigDecimal.TEN,
                LocalDate.of(2026, 9, 7),
                PaymentMethod.CASH,
                null,
                null,
                ReceiptStatus.NO_RECEIPT,
                null,
                null
        );
    }

    private void handleRequest(HttpExchange exchange) {
        requestPath.set(exchange.getRequestURI().getRawQuery());
        requestBody.set(readBody(exchange));
        respond(exchange, 200, "[]");
    }

    private String readBody(HttpExchange exchange) {
        try {
            return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private void respond(HttpExchange exchange, int status, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private String expenseJson(Expense expense) {
        return """
                [{
                  "id": "%s",
                  "user_id": "%s",
                  "category_id": "%s",
                  "merchant": "%s",
                  "amount": "%s",
                  "expense_date": "%s",
                  "payment_method": "%s",
                  "description": null,
                  "receipt_path": null,
                  "receipt_status": "NO_RECEIPT",
                  "created_at": null,
                  "updated_at": null
                }]
                """.formatted(
                expense.id(),
                expense.userId(),
                expense.categoryId(),
                expense.merchant(),
                expense.amount(),
                expense.expenseDate(),
                expense.paymentMethod().name()
        );
    }
}