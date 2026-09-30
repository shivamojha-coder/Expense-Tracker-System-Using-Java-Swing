package com.expensetracker.service;

import com.expensetracker.model.SupabaseSession;
import com.expensetracker.model.User;
import com.expensetracker.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

public final class SupabaseAuthService implements AuthService {
    private final SupabaseClient client;
    private final ObjectMapper objectMapper;
    private SupabaseSession session;

    public SupabaseAuthService(SupabaseClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public synchronized User login(String email, char[] password) {
        return authenticate(email, password, "/auth/v1/token?grant_type=password");
    }

    @Override
    public synchronized User register(String email, char[] password) {
        String passwordValue = new String(password);
        Arrays.fill(password, '\0');
        try {
            String body = objectMapper.createObjectNode()
                    .put("email", email)
                    .put("password", passwordValue)
                    .toString();
            var response = client.send("/auth/v1/signup", "POST", body, null);
            if (!isSuccessful(response.statusCode())) {
                throw new ServiceException(readError(response.body(), "Unable to create the account."));
            }

            JsonNode payload = parse(response.body());
            JsonNode userNode = payload.path("user");
            if (userNode.isMissingNode() || userNode.path("id").asText().isBlank()) {
                // Projects that require email confirmation return the user
                // object directly at the top level instead of nested under
                // "user" (no session is created until the address is confirmed).
                userNode = payload;
            }
            if (userNode.path("id").asText().isBlank()) {
                throw new ServiceException("Supabase returned an invalid registration response.");
            }

            String accessToken = payload.path("access_token").asText("");
            if (!accessToken.isBlank()) {
                session = toSession(payload, userNode);
            }
            return toUser(userNode);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ServiceException("Supabase returned an invalid registration response.", exception);
        } finally {
            passwordValue = null;
        }
    }

    @Override
    public synchronized void logout() {
        if (session != null && session.accessToken() != null && !session.accessToken().isBlank()) {
            client.send("/auth/v1/logout", "POST", "{}", session.accessToken());
        }
        session = null;
    }

    @Override
    public synchronized User getCurrentUser() {
        return session == null ? null : session.user();
    }

    public synchronized SupabaseSession getCurrentSession() {
        return session;
    }

    @Override
    public synchronized String getAccessToken() {
        return session == null ? "" : session.accessToken();
    }

    private User authenticate(String email, char[] password, String endpoint) {
        String passwordValue = new String(password);
        Arrays.fill(password, '\0');
        try {
            String body = objectMapper.createObjectNode()
                    .put("email", email)
                    .put("password", passwordValue)
                    .toString();
            var response = client.send(endpoint, "POST", body, null);
            if (!isSuccessful(response.statusCode())) {
                throw new ServiceException(readError(response.body(), "Unable to sign in."));
            }
            JsonNode payload = parse(response.body());
            JsonNode userNode = payload.path("user");
            if (userNode.isMissingNode() || payload.path("access_token").asText().isBlank()) {
                throw new ServiceException("Supabase returned an invalid login response.");
            }
            session = toSession(payload, userNode);
            return session.user();
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ServiceException("Supabase returned an invalid login response.", exception);
        } finally {
            passwordValue = null;
        }
    }

    private SupabaseSession toSession(JsonNode payload, JsonNode userNode) {
        long expiresIn = payload.path("expires_in").asLong(3600);
        return new SupabaseSession(
                payload.path("access_token").asText(),
                payload.path("refresh_token").asText(""),
                Instant.now().plusSeconds(expiresIn),
                toUser(userNode)
        );
    }

    private User toUser(JsonNode userNode) {
        String id = userNode.path("id").asText("");
        try {
            return new User(UUID.fromString(id), userNode.path("email").asText(""));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("Supabase returned an invalid user identifier.", exception);
        }
    }

    private JsonNode parse(String body) throws com.fasterxml.jackson.core.JsonProcessingException {
        return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
    }

    private String readError(String body, String fallback) {
        try {
            JsonNode payload = parse(body);
            String message = firstText(payload, "msg", "message", "error_description", "error");
            return message.isBlank() ? fallback : message;
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return fallback;
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}