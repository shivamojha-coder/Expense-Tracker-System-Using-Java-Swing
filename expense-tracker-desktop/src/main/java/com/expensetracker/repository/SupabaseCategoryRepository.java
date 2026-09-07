package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.service.ServiceException;
import com.expensetracker.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class SupabaseCategoryRepository implements CategoryRepository {
    private final SupabaseClient client;
    private final Supplier<String> accessTokenSupplier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseCategoryRepository(SupabaseClient client, Supplier<String> accessTokenSupplier) {
        this.client = client;
        this.accessTokenSupplier = accessTokenSupplier;
    }

    @Override
    public List<Category> findAll() {
        HttpResponse<String> response = client.send(
                "/rest/v1/categories?select=id,name&order=name.asc",
                "GET",
                null,
                accessToken()
        );
        ensureSuccess(response);
        JsonNode rows = parse(response.body());
        if (!rows.isArray()) {
            throw new ServiceException("Supabase returned invalid category data.");
        }
        List<Category> categories = new ArrayList<>();
        for (JsonNode row : rows) {
            try {
                categories.add(new Category(UUID.fromString(row.path("id").asText()), row.path("name").asText()));
            } catch (IllegalArgumentException exception) {
                throw new ServiceException("Supabase returned an invalid category.", exception);
            }
        }
        return categories;
    }

    @Override
    public Optional<Category> findById(UUID categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return findAll().stream().filter(category -> category.id().equals(categoryId)).findFirst();
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body == null || body.isBlank() ? "[]" : body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ServiceException("Supabase returned invalid category data.", exception);
        }
    }

    private void ensureSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServiceException("Unable to load categories.");
        }
    }

    private String accessToken() {
        String token = accessTokenSupplier == null ? "" : accessTokenSupplier.get();
        if (token == null || token.isBlank()) {
            throw new ServiceException("Your session has expired. Please sign in again.");
        }
        return token;
    }
}