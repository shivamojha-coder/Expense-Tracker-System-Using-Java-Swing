package com.expensetracker.supabase;

import com.expensetracker.config.AppConfig;
import com.expensetracker.service.ServiceException;

import java.net.URI;
import java.net.http.HttpClient;

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
}