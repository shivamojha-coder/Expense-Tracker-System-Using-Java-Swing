package com.expensetracker.supabase;

import com.expensetracker.config.AppConfig;
import com.expensetracker.service.ServiceException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class SupabaseClient {
    private final AppConfig config;
    private final HttpClient httpClient;

    public SupabaseClient(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    public AppConfig config() {
        return config;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public URI endpoint(String path) {
        if (!config.isSupabaseConfigured()) {
            throw new ServiceException("Supabase is not configured.");
        }
        String base = config.supabaseUrl().replaceAll("/+$", "");
        return URI.create(base + "/" + path.replaceAll("^/+", ""));
    }

    public HttpResponse<String> send(String path, String method, String body, String accessToken) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint(path))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", config.supabaseAnonKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        if (accessToken != null && !accessToken.isBlank()) {
            request.header("Authorization", "Bearer " + accessToken);
        }

        if ("POST".equalsIgnoreCase(method)) {
            request.header("Prefer", "return=representation");
            request.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("PUT".equalsIgnoreCase(method)) {
            request.method("PUT", HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("PATCH".equalsIgnoreCase(method)) {
            request.header("Prefer", "return=representation");
            request.method("PATCH", HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("DELETE".equalsIgnoreCase(method)) {
            request.header("Prefer", "return=representation");
            request.DELETE();
        } else if ("GET".equalsIgnoreCase(method)) {
            request.GET();
        } else {
            throw new ServiceException("Unsupported Supabase HTTP method: " + method);
        }

        try {
            return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ServiceException("The Supabase request was interrupted.", interrupted);
        } catch (java.io.IOException exception) {
            throw new ServiceException("Unable to reach Supabase (" + exception.getMessage() + "). Check connection.", exception);
        }
    }
}