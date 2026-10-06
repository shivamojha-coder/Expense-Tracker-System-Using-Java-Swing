package com.expensetracker.supabase;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.SupabaseExpenseRepository;
import com.expensetracker.service.StorageService;
import com.expensetracker.service.SupabaseAuthService;
import com.expensetracker.service.SupabaseStorageService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the real HTTP clients against a local fake of the Supabase endpoints. */
class SupabaseHttpTest {
    private static final UUID USER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private HttpServer server;
    private SupabaseClient client;
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        client = new SupabaseClient(new AppConfig(
                "http://127.0.0.1:" + server.getAddress().getPort(), "anon-key", "expense-receipts", 10 * 1024 * 1024));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static String body(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String session(String access, String refresh, int expiresIn) {
        return "{\"access_token\":\"" + access + "\",\"refresh_token\":\"" + refresh + "\",\"expires_in\":" + expiresIn
                + ",\"user\":{\"id\":\"" + USER + "\",\"email\":\"a@b.co\",\"user_metadata\":{\"display_name\":\"Ann\"}}}";
    }

    @Test
    void expiringAccessTokenIsRefreshedAutomatically() {
        server.createContext("/auth/v1/token", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            requests.add(query + " " + body(exchange));
            reply(exchange, 200, query.contains("refresh_token")
                    ? session("fresh-token", "refresh-2", 3600)
                    : session("short-lived", "refresh-1", 5));
        });
        server.start();

        SupabaseAuthService auth = new SupabaseAuthService(client);
        auth.login("a@b.co", "secret".toCharArray());

        assertEquals("fresh-token", auth.getAccessToken());
        assertEquals("fresh-token", auth.getAccessToken());
        assertEquals(2, requests.size(), "one login and exactly one refresh");
        assertTrue(requests.get(1).contains("\"refresh_token\":\"refresh-1\""));
        assertEquals("Ann", auth.getDisplayName());
    }

    @Test
    void rejectedRefreshTokenEndsTheSession() {
        server.createContext("/auth/v1/token", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            body(exchange);
            if (query.contains("refresh_token")) {
                reply(exchange, 400, "{\"error\":\"invalid_grant\"}");
            } else {
                reply(exchange, 200, session("short-lived", "refresh-1", 5));
            }
        });
        server.start();

        SupabaseAuthService auth = new SupabaseAuthService(client);
        auth.login("a@b.co", "secret".toCharArray());

        assertEquals("", auth.getAccessToken());
    }

    @Test
    void receiptDeletionUsesHttpDelete() {
        server.createContext("/storage/v1/object/expense-receipts", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " " + body(exchange));
            reply(exchange, 200, "[]");
        });
        server.start();

        StorageService storage = new SupabaseStorageService(client, () -> "token", "expense-receipts", USER, 1024);
        storage.deleteReceipt(USER + "/expense-1/receipt.png");

        assertEquals(1, requests.size());
        assertTrue(requests.get(0).startsWith("DELETE /storage/v1/object/expense-receipts "), requests.get(0));
        assertTrue(requests.get(0).contains("\"prefixes\":[\"" + USER + "/expense-1/receipt.png\"]"), requests.get(0));
    }

    @Test
    void expensesAreReadInPagesSoNothingIsDropped() {
        server.createContext("/rest/v1/expenses", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            requests.add(query);
            int offset = Integer.parseInt(query.replaceAll(".*offset=(\\d+).*", "$1"));
            int count = offset == 0 ? 500 : 3;
            StringBuilder rows = new StringBuilder("[");
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    rows.append(',');
                }
                rows.append("{\"id\":\"").append(UUID.randomUUID())
                        .append("\",\"user_id\":\"").append(USER)
                        .append("\",\"category_id\":\"").append(UUID.randomUUID())
                        .append("\",\"merchant\":\"Shop\",\"amount\":\"12.50\",\"expense_date\":\"2026-09-01\"")
                        .append(",\"payment_method\":\"CASH\",\"receipt_status\":\"NO_RECEIPT\"}");
            }
            reply(exchange, 200, rows.append(']').toString());
        });
        server.start();

        SupabaseExpenseRepository repository = new SupabaseExpenseRepository(client, () -> "token", USER);
        List<Expense> expenses = repository.findByUser(USER);

        assertEquals(503, expenses.size());
        assertEquals(2, requests.size());
        assertTrue(requests.get(0).contains("offset=0"));
        assertTrue(requests.get(1).contains("offset=500"));
        assertTrue(requests.get(0).contains("order=expense_date.desc,created_at.desc,id.asc"));
    }
}
