package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.ServiceException;
import com.expensetracker.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * PostgREST implementation of the expense repository.
 *
 * <p>The user id is part of every query, including mutations. Supabase RLS is
 * the authoritative boundary, while this additional predicate prevents the
 * desktop client from accidentally addressing an unowned row.</p>
 */
public final class SupabaseExpenseRepository implements ExpenseRepository {
    private static final int PAGE_SIZE = 500;

    private final SupabaseClient client;
    private final Supplier<String> accessTokenSupplier;
    private final UUID userId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseExpenseRepository(
            SupabaseClient client,
            Supplier<String> accessTokenSupplier,
            UUID userId
    ) {
        this.client = client;
        this.accessTokenSupplier = accessTokenSupplier;
        this.userId = userId;
    }

    @Override
    public Expense create(Expense expense) {
        requireOwned(expense);
        HttpResponse<String> response = client.send(
                "/rest/v1/expenses",
                "POST",
                toPayload(expense).toString(),
                accessToken()
        );
        return parseMutation(response, "Unable to create the expense.");
    }

    @Override
    public Optional<Expense> findById(UUID expenseId) {
        requireId(expenseId);
        HttpResponse<String> response = client.send(
                expensePath("id=eq." + expenseId),
                "GET",
                null,
                accessToken()
        );
        ensureSuccess(response, "Unable to load the expense.");
        JsonNode rows = parse(response.body(), "Supabase returned invalid expense data.");
        if (!rows.isArray() || rows.isEmpty()) {
            return Optional.empty();
        }
        Expense expense = fromJson(rows.get(0));
        requireReturnedOwned(expense);
        return Optional.of(expense);
    }

    @Override
    public List<Expense> findByUser(UUID requestedUserId) {
        if (!userId.equals(requestedUserId)) {
            throw new ServiceException("You can only view your own expenses.");
        }
        // PostgREST returns at most its configured page size (1000 by default) per request,
        // so read page by page; otherwise users with many expenses silently lose the oldest ones.
        List<Expense> expenses = new ArrayList<>();
        for (int offset = 0; ; offset += PAGE_SIZE) {
            HttpResponse<String> response = client.send(
                    expensePath("limit=" + PAGE_SIZE + "&offset=" + offset),
                    "GET",
                    null,
                    accessToken()
            );
            ensureSuccess(response, "Unable to load expenses.");
            JsonNode rows = parse(response.body(), "Supabase returned invalid expense data.");
            if (!rows.isArray()) {
                throw new ServiceException("Supabase returned invalid expense data.");
            }
            for (JsonNode row : rows) {
                Expense expense = fromJson(row);
                requireReturnedOwned(expense);
                expenses.add(expense);
            }
            if (rows.size() < PAGE_SIZE) {
                return expenses;
            }
        }
    }

    @Override
    public Expense update(Expense expense) {
        requireOwned(expense);
        requireId(expense.id());
        HttpResponse<String> response = client.send(
                expensePath("id=eq." + expense.id()),
                "PATCH",
                toPayload(expense).toString(),
                accessToken()
        );
        return parseMutation(response, "Unable to update the expense.");
    }

    @Override
    public void delete(UUID expenseId) {
        requireId(expenseId);
        HttpResponse<String> response = client.send(
                expensePath("id=eq." + expenseId),
                "DELETE",
                null,
                accessToken()
        );
        ensureSuccess(response, "Unable to delete the expense.");
        if (response.statusCode() == 204
                || response.body() == null
                || response.body().isBlank()) {
            return;
        }
        JsonNode deletedRows = parse(response.body(), "Supabase returned invalid delete data.");
        if (deletedRows.isArray() && deletedRows.isEmpty()) {
            throw new ServiceException("The expense was not found or could not be deleted.");
        }
    }

    private String expensePath(String additionalFilter) {
        StringBuilder path = new StringBuilder(
                "/rest/v1/expenses?select=*&user_id=eq."
        ).append(userId);
        if (!additionalFilter.isBlank()) {
            path.append('&').append(additionalFilter);
        }
        // "id" makes the order total, so paging never repeats or skips rows that share a date and timestamp.
        path.append("&order=expense_date.desc,created_at.desc,id.asc");
        return path.toString();
    }

    private ObjectNode toPayload(Expense expense) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("user_id", expense.userId().toString());
        payload.put("category_id", expense.categoryId().toString());
        payload.put("merchant", expense.merchant().trim());
        payload.put("amount", expense.amount());
        payload.put("expense_date", expense.expenseDate().toString());
        payload.put("payment_method", expense.paymentMethod().name());
        if (expense.description() == null || expense.description().isBlank()) {
            payload.putNull("description");
        } else {
            payload.put("description", expense.description().trim());
        }
        if (expense.receiptPath() == null || expense.receiptPath().isBlank()) {
            payload.putNull("receipt_path");
        } else {
            payload.put("receipt_path", expense.receiptPath());
        }
        payload.put("receipt_status", expense.receiptStatus().name());
        return payload;
    }

    private Expense parseMutation(HttpResponse<String> response, String fallback) {
        ensureSuccess(response, fallback);
        JsonNode rows = parse(response.body(), "Supabase returned invalid expense data.");
        if (!rows.isArray() || rows.isEmpty()) {
            throw new ServiceException(fallback);
        }
        Expense expense = fromJson(rows.get(0));
        requireReturnedOwned(expense);
        return expense;
    }

    private Expense fromJson(JsonNode node) {
        try {
            return new Expense(
                    UUID.fromString(requiredText(node, "id")),
                    UUID.fromString(requiredText(node, "user_id")),
                    UUID.fromString(requiredText(node, "category_id")),
                    requiredText(node, "merchant"),
                    new BigDecimal(requiredText(node, "amount")),
                    LocalDate.parse(requiredText(node, "expense_date")),
                    parsePaymentMethod(requiredText(node, "payment_method")),
                    nullableText(node, "description"),
                    nullableText(node, "receipt_path"),
                    parseReceiptStatus(node.path("receipt_status").asText("NO_RECEIPT")),
                    parseInstant(node, "created_at"),
                    parseInstant(node, "updated_at")
            );
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("Supabase returned an invalid expense record.", exception);
        }
    }

    private PaymentMethod parsePaymentMethod(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.name().equals(normalized)
                    || method.displayName().toUpperCase().replace(' ', '_').equals(normalized)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown payment method");
    }

    private ReceiptStatus parseReceiptStatus(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (ReceiptStatus status : ReceiptStatus.values()) {
            if (status.name().equals(normalized)
                    || status.displayName().toUpperCase().replace(' ', '_').equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown receipt status");
    }

    private Instant parseInstant(JsonNode node, String field) {
        String value = nullableText(node, field);
        return value == null ? null : Instant.parse(value);
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value;
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private JsonNode parse(String body, String message) {
        try {
            return objectMapper.readTree(body == null || body.isBlank() ? "[]" : body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ServiceException(message, exception);
        }
    }

    private void ensureSuccess(HttpResponse<String> response, String fallback) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServiceException(readError(response.body(), fallback));
        }
    }

    private String readError(String body, String fallback) {
        try {
            JsonNode payload = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
            for (String field : List.of("message", "hint", "details", "msg", "error")) {
                String value = payload.path(field).asText("");
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // Keep the user-facing fallback when Supabase does not return JSON.
        }
        return fallback;
    }

    private String accessToken() {
        String token = accessTokenSupplier == null ? "" : accessTokenSupplier.get();
        if (token == null || token.isBlank()) {
            throw new ServiceException("Your session has expired. Please sign in again.");
        }
        return token;
    }

    private void requireOwned(Expense expense) {
        if (expense == null || !userId.equals(expense.userId())) {
            throw new ServiceException("You can only change your own expenses.");
        }
    }

    private void requireReturnedOwned(Expense expense) {
        if (expense == null || !userId.equals(expense.userId())) {
            throw new ServiceException("Supabase returned an expense belonging to another user.");
        }
    }

    private void requireId(UUID id) {
        if (id == null) {
            throw new ServiceException("Expense identifier is required.");
        }
    }
}