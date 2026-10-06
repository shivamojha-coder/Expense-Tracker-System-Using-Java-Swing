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
    private String displayName = "";

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
    public void sendPasswordReset(String email) {
        String body = objectMapper.createObjectNode().put("email", email).toString();
        var response = client.send("/auth/v1/recover", "POST", body, null);
        if (!isSuccessful(response.statusCode())) {
            throw new ServiceException(readError(response.body(), "Unable to send the password reset email."));
        }
    }

    @Override
    public synchronized String getDisplayName() {
        return displayName;
    }

    @Override
    public void updateDisplayName(String newName) {
        String name = newName == null ? "" : newName.trim();
        var data = objectMapper.createObjectNode();
        data.putObject("data").put("display_name", name);
        var response = client.send("/auth/v1/user", "PUT", data.toString(), requireToken());
        if (!isSuccessful(response.statusCode())) {
            throw new ServiceException(readError(response.body(), "Unable to save the display name."));
        }
        synchronized (this) {
            displayName = name;
        }
    }

    @Override
    public void changePassword(char[] newPassword) {
        String passwordValue = new String(newPassword);
        Arrays.fill(newPassword, '\0');
        try {
            String body = objectMapper.createObjectNode().put("password", passwordValue).toString();
            var response = client.send("/auth/v1/user", "PUT", body, requireToken());
            if (!isSuccessful(response.statusCode())) {
                throw new ServiceException(readError(response.body(), "Unable to change the password."));
            }
        } finally {
            passwordValue = null;
        }
    }

    @Override
    public void deleteAccount(boolean confirm) {
        String body = objectMapper.createObjectNode().put("confirm", confirm).toString();
        var response = client.send("/rest/v1/rpc/delete_my_account", "POST", body, requireToken());
        if (response.statusCode() == 404) {
            throw new ServiceException(
                    "Account deletion is not set up in your database yet. Run the updated supabase/schema.sql in the Supabase SQL editor.");
        }
        if (!isSuccessful(response.statusCode())) {
            throw new ServiceException(readError(response.body(), "Unable to delete the account."));
        }
    }

    private String requireToken() {
        String token = getAccessToken();
        if (token == null || token.isBlank()) {
            throw new ServiceException("Your session has expired. Please sign in again.");
        }
        return token;
    }

    @Override
    public synchronized void logout() {
        String token = session == null ? "" : session.accessToken();
        session = null;
        displayName = "";
        if (token != null && !token.isBlank()) {
            // Revoking the token is best effort; do not block the caller (often the UI thread) on the network.
            Thread revoke = new Thread(() -> {
                try {
                    client.send("/auth/v1/logout", "POST", "{}", token);
                } catch (ServiceException ignored) {
                    // The local session is already cleared; the token simply expires on its own.
                }
            }, "supabase-logout");
            revoke.setDaemon(true);
            revoke.start();
        }
    }

    @Override
    public synchronized User getCurrentUser() {
        return session == null ? null : session.user();
    }

    /** Returns a valid access token, renewing it first when it is about to expire. */
    @Override
    public synchronized String getAccessToken() {
        if (session != null && needsRefresh(session)) {
            refreshSession();
        }
        return session == null ? "" : session.accessToken();
    }

    private static boolean needsRefresh(SupabaseSession current) {
        return current.refreshToken() != null && !current.refreshToken().isBlank()
                && Instant.now().isAfter(current.expiresAt().minusSeconds(60));
    }

    /**
     * Exchanges the refresh token for a new access token. A rejected refresh token ends the session;
     * a network or server hiccup keeps the current token so the next call can try again.
     */
    private void refreshSession() {
        try {
            String body = objectMapper.createObjectNode().put("refresh_token", session.refreshToken()).toString();
            var response = client.send("/auth/v1/token?grant_type=refresh_token", "POST", body, null);
            int status = response.statusCode();
            if (status >= 400 && status < 500) {
                session = null;
                displayName = "";
                return;
            }
            if (!isSuccessful(status)) {
                return;
            }
            JsonNode payload = parse(response.body());
            if (payload.path("access_token").asText().isBlank()) {
                return;
            }
            JsonNode userNode = payload.path("user");
            session = toSession(payload, userNode.isMissingNode() ? userNodeOf(session.user()) : userNode);
        } catch (ServiceException | com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // Keep the existing token; the request that needed it will report the real problem.
        }
    }

    private JsonNode userNodeOf(User user) {
        return objectMapper.createObjectNode().put("id", user.id().toString()).put("email", user.email());
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
        displayName = userNode.path("user_metadata").path("display_name").asText("");
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