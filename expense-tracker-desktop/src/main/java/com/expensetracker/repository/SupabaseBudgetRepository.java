package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import com.expensetracker.service.ServiceException;
import com.expensetracker.supabase.SupabaseClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** PostgREST implementation; row-level security restricts every row to its owner. */
public final class SupabaseBudgetRepository implements BudgetRepository {
    private static final String MISSING_TABLE =
            "Budgets are not set up in your database yet. Run the updated supabase/schema.sql in the Supabase SQL editor.";

    private final SupabaseClient client;
    private final Supplier<String> accessTokenSupplier;
    private final UUID userId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseBudgetRepository(SupabaseClient client, Supplier<String> accessTokenSupplier, UUID userId) {
        this.client = client;
        this.accessTokenSupplier = accessTokenSupplier;
        this.userId = userId;
    }

    @Override
    public List<Budget> findAll() {
        HttpResponse<String> response = client.send(
                "/rest/v1/budgets?select=id,category_id,monthly_limit&user_id=eq." + userId,
                "GET", null, accessToken());
        ensureSuccess(response, "Unable to load budgets.");
        JsonNode rows = parse(response.body());
        List<Budget> budgets = new ArrayList<>();
        for (JsonNode row : rows) {
            budgets.add(fromJson(row));
        }
        return budgets;
    }

    @Override
    public Budget save(UUID categoryId, BigDecimal monthlyLimit) {
        Budget existing = findAll().stream()
                .filter(budget -> Objects.equals(budget.categoryId(), categoryId))
                .findFirst()
                .orElse(null);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("monthly_limit", monthlyLimit);
        HttpResponse<String> response;
        if (existing == null) {
            payload.put("user_id", userId.toString());
            if (categoryId == null) {
                payload.putNull("category_id");
            } else {
                payload.put("category_id", categoryId.toString());
            }
            response = client.send("/rest/v1/budgets", "POST", payload.toString(), accessToken());
        } else {
            response = client.send(
                    "/rest/v1/budgets?id=eq." + existing.id() + "&user_id=eq." + userId,
                    "PATCH", payload.toString(), accessToken());
        }
        ensureSuccess(response, "Unable to save the budget.");
        JsonNode rows = parse(response.body());
        if (!rows.isArray() || rows.isEmpty()) {
            throw new ServiceException("Unable to save the budget.");
        }
        return fromJson(rows.get(0));
    }

    @Override
    public void delete(UUID budgetId) {
        HttpResponse<String> response = client.send(
                "/rest/v1/budgets?id=eq." + budgetId + "&user_id=eq." + userId,
                "DELETE", null, accessToken());
        ensureSuccess(response, "Unable to remove the budget.");
    }

    private Budget fromJson(JsonNode row) {
        try {
            JsonNode category = row.get("category_id");
            UUID categoryId = category == null || category.isNull() ? null : UUID.fromString(category.asText());
            return new Budget(
                    UUID.fromString(row.path("id").asText()),
                    categoryId,
                    new BigDecimal(row.path("monthly_limit").asText())
            );
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("Supabase returned an invalid budget.", exception);
        }
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body == null || body.isBlank() ? "[]" : body);
        } catch (JsonProcessingException exception) {
            throw new ServiceException("Supabase returned invalid budget data.", exception);
        }
    }

    private void ensureSuccess(HttpResponse<String> response, String fallback) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 404) {
            throw new ServiceException(MISSING_TABLE);
        }
        try {
            JsonNode payload = objectMapper.readTree(response.body() == null || response.body().isBlank() ? "{}" : response.body());
            String message = payload.path("message").asText("");
            if (!message.isBlank()) {
                throw new ServiceException(message);
            }
        } catch (JsonProcessingException ignored) {
            // Fall back to the generic message below.
        }
        throw new ServiceException(fallback);
    }

    private String accessToken() {
        String token = accessTokenSupplier == null ? "" : accessTokenSupplier.get();
        if (token == null || token.isBlank()) {
            throw new ServiceException("Your session has expired. Please sign in again.");
        }
        return token;
    }
}
