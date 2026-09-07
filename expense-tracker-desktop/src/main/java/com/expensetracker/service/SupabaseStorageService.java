package com.expensetracker.service;

import com.expensetracker.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Supabase Storage adapter for private receipt objects.
 *
 * <p>Receipt paths are scoped below the authenticated user's UUID. This is a
 * client-side guard in addition to the bucket policy/RLS configuration.</p>
 */
public final class SupabaseStorageService implements StorageService {
    private final SupabaseClient client;
    private final Supplier<String> accessTokenSupplier;
    private final String bucket;
    private final UUID userId;
    private final long maxReceiptSizeBytes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseStorageService(
            SupabaseClient client,
            Supplier<String> accessTokenSupplier,
            String bucket,
            UUID userId,
            long maxReceiptSizeBytes
    ) {
        this.client = client;
        this.accessTokenSupplier = accessTokenSupplier;
        this.bucket = bucket == null || bucket.isBlank() ? "expense-receipts" : bucket.trim();
        this.userId = userId;
        this.maxReceiptSizeBytes = maxReceiptSizeBytes;
        if (userId == null) {
            throw new IllegalArgumentException("User is required for receipt storage.");
        }
    }

    public SupabaseStorageService(
            SupabaseClient client,
            Supplier<String> accessTokenSupplier,
            String bucket,
            UUID userId
    ) {
        this(client, accessTokenSupplier, bucket, userId, 10 * 1024 * 1024);
    }

    @Override
    public String uploadReceipt(UUID expenseId, Path localFile) {
        if (expenseId == null) {
            throw new ServiceException("Expense must be saved before attaching a receipt.");
        }
        ReceiptFileValidator.validate(localFile, maxReceiptSizeBytes);
        try {
            String path = userId + "/" + expenseId + "/" + UUID.randomUUID()
                    + "." + ReceiptFileValidator.extension(localFile);
            HttpRequest request = baseRequest(objectPath(path))
                    .header("Content-Type", ReceiptFileValidator.contentType(localFile))
                    .header("Cache-Control", "3600")
                    .header("x-upsert", "false")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(localFile)))
                    .build();
            HttpResponse<String> response = send(request);
            ensureSuccess(response, "Receipt upload failed.");
            return path;
        } catch (IOException exception) {
            throw new ServiceException("The receipt could not be read for upload.", exception);
        }
    }

    @Override
    public byte[] downloadReceipt(String receiptPath) {
        requireOwnedPath(receiptPath);
        HttpRequest request = baseRequest(objectPath(receiptPath)).GET().build();
        try {
            HttpResponse<byte[]> response = client.httpClient().send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException("Receipt preview could not be loaded: "
                        + readError(new String(response.body(), StandardCharsets.UTF_8)));
            }
            return response.body();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Receipt preview was interrupted.", interrupted);
        } catch (IOException exception) {
            throw new ServiceException("Unable to load the receipt preview.", exception);
        }
    }

    @Override
    public void deleteReceipt(String receiptPath) {
        requireOwnedPath(receiptPath);
        String body;
        try {
            body = objectMapper.createObjectNode()
                    .putArray("prefixes")
                    .add(receiptPath)
                    .toString();
        } catch (RuntimeException exception) {
            throw new ServiceException("The receipt could not be removed.", exception);
        }
        HttpRequest request = baseRequest(
                "/storage/v1/object/" + encodeSegment(bucket)
        ).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        ensureSuccess(send(request), "Receipt removal failed.");
    }

    private HttpRequest.Builder baseRequest(String path) {
        String accessToken = accessTokenSupplier == null ? "" : accessTokenSupplier.get();
        if (accessToken == null || accessToken.isBlank()) {
            throw new ServiceException("Your session has expired. Please sign in again.");
        }
        return HttpRequest.newBuilder(client.endpoint(path))
                .timeout(Duration.ofSeconds(30))
                .header("apikey", client.config().supabaseAnonKey())
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json");
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ServiceException("The receipt request was interrupted.", interrupted);
        } catch (IOException exception) {
            throw new ServiceException("Unable to reach Supabase Storage.", exception);
        }
    }

    private void ensureSuccess(HttpResponse<String> response, String fallback) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServiceException(fallback + " " + readError(response.body()));
        }
    }

    private String readError(String body) {
        try {
            JsonNode payload = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
            for (String field : new String[]{"message", "error", "statusCode"}) {
                String value = payload.path(field).asText("");
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // The fallback is more useful than exposing an HTML/proxy response.
        }
        return "Please try again.";
    }

    private String objectPath(String path) {
        requireOwnedPath(path);
        return "/storage/v1/object/" + encodeSegment(bucket) + "/" + encodePath(path);
    }

    private void requireOwnedPath(String path) {
        if (path == null || path.isBlank()) {
            throw new ServiceException("Receipt path is required.");
        }
        String prefix = userId + "/";
        if (!path.startsWith(prefix) || path.contains("..") || path.contains("//")) {
            throw new ServiceException("You can only access your own receipt.");
        }
    }

    private String encodePath(String path) {
        return java.util.Arrays.stream(path.split("/", -1))
                .map(this::encodeSegment)
                .reduce((left, right) -> left + "/" + right)
                .orElseThrow(() -> new ServiceException("Receipt path is required."));
    }

    private String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}