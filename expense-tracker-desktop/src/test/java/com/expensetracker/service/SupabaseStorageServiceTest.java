package com.expensetracker.service;

import com.expensetracker.config.AppConfig;
import com.expensetracker.supabase.SupabaseClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupabaseStorageServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID expenseId = UUID.randomUUID();
    private HttpServer server;
    private AtomicReference<String> method;
    private AtomicReference<String> requestPath;
    private AtomicReference<String> requestBody;
    private AtomicReference<byte[]> requestBytes;
    private AtomicInteger responseStatus;
    private SupabaseStorageService storage;

    @BeforeEach
    void setUp() throws IOException {
        method = new AtomicReference<>();
        requestPath = new AtomicReference<>();
        requestBody = new AtomicReference<>();
        requestBytes = new AtomicReference<>();
        responseStatus = new AtomicInteger(200);
        capturedHeaders = null;
        response = null;
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/storage/v1/object/", this::handleStorageRequest);
        server.start();
        AppConfig config = new AppConfig(
                "http://localhost:" + server.getAddress().getPort(),
                "test-anon-key",
                "expense-receipts"
        );
        storage = new SupabaseStorageService(
                new SupabaseClient(config),
                () -> "test-access-token",
                config.storageBucket(),
                userId,
                1024
        );
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void uploadsReceiptToUserAndExpenseScopedPath() throws IOException {
        Path receipt = writeJpeg();

        String path = storage.uploadReceipt(expenseId, receipt);

        assertTrue(path.startsWith(userId + "/" + expenseId + "/"));
        assertTrue(path.endsWith(".jpg"));
        assertEquals("POST", method.get());
        assertEquals("/storage/v1/object/expense-receipts/" + path, requestPath.get());
        assertEquals("image/jpeg", requestHeader("Content-Type"));
        assertEquals("Bearer test-access-token", requestHeader("Authorization"));
        assertArrayEquals(Files.readAllBytes(receipt), requestBytes.get());
    }

    @Test
    void downloadsPreviewBytesForOwnedPath() {
        byte[] preview = new byte[]{1, 2, 3};
        responseBody(preview);
        String path = userId + "/" + expenseId + "/receipt.png";

        assertArrayEquals(preview, storage.downloadReceipt(path));

        assertEquals("GET", method.get());
        assertEquals("/storage/v1/object/expense-receipts/" + path, requestPath.get());
    }

    @Test
    void removesOwnedReceiptWithStorageDeletePayload() {
        String path = userId + "/" + expenseId + "/receipt.jpg";

        storage.deleteReceipt(path);

        assertEquals("POST", method.get());
        assertEquals("/storage/v1/object/expense-receipts", requestPath.get());
        assertEquals("[\"" + path + "\"]", requestBody.get());
    }

    @Test
    void reportsRecoverableUploadFailure() throws IOException {
        responseStatus.set(500);
        responseBody("storage unavailable");

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> storage.uploadReceipt(expenseId, writeJpeg())
        );

        assertTrue(exception.getMessage().contains("Receipt upload failed."));
        assertTrue(exception.getMessage().contains("Please try again."));
    }

    @Test
    void rejectsForeignReceiptPathsBeforeMakingRequests() {
        String foreignPath = otherUserId + "/" + expenseId + "/receipt.jpg";

        assertThrows(ServiceException.class, () -> storage.downloadReceipt(foreignPath));
        assertThrows(ServiceException.class, () -> storage.deleteReceipt(foreignPath));

        assertNull(method.get());
    }

    @Test
    void rejectsMissingExpenseIdBeforeUpload() throws IOException {
        assertThrows(ServiceException.class, () -> storage.uploadReceipt(null, writeJpeg()));
        assertNull(method.get());
    }

    private Path writeJpeg() throws IOException {
        Path receipt = Files.createTempFile("receipt-test-", ".jpg");
        Files.write(receipt, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
        receipt.toFile().deleteOnExit();
        return receipt;
    }

    private void handleStorageRequest(HttpExchange exchange) {
        method.set(exchange.getRequestMethod());
        requestPath.set(exchange.getRequestURI().getRawPath());
        capturedHeaders = exchange.getRequestHeaders();
        try {
            byte[] request = exchange.getRequestBody().readAllBytes();
            requestBytes.set(request);
            requestBody.set(new String(request, StandardCharsets.UTF_8));
            byte[] response = responseBodyBytes();
            exchange.sendResponseHeaders(responseStatus.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private String requestHeader(String name) {
        return capturedHeaders == null ? null : capturedHeaders.getFirst(name);
    }

    private com.sun.net.httpserver.Headers capturedHeaders;

    private void responseBody(String body) {
        responseBody(body.getBytes(StandardCharsets.UTF_8));
    }

    private void responseBody(byte[] body) {
        this.response = body;
    }

    private byte[] responseBodyBytes() {
        return response == null ? "{}".getBytes(StandardCharsets.UTF_8) : response;
    }

    private byte[] response;
}