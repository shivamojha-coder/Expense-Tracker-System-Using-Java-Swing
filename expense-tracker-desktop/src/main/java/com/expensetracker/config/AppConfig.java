package com.expensetracker.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AppConfig(
        String supabaseUrl,
        String supabaseAnonKey,
        String storageBucket,
        long maxReceiptSizeBytes
) {
    private static final long DEFAULT_MAX_RECEIPT_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Map<String, String> ENV_FILE_CACHE = loadDotEnv();

    public AppConfig(String supabaseUrl, String supabaseAnonKey, String storageBucket) {
        this(supabaseUrl, supabaseAnonKey, storageBucket, DEFAULT_MAX_RECEIPT_SIZE_BYTES);
    }

    public static AppConfig fromEnvironment() {
        return new AppConfig(
                readOrDefault("SUPABASE_URL", "https://gyqujxvriciejibrmsjq.supabase.co"),
                readOrDefault("SUPABASE_ANON_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd5cXVqeHZyaWNpZWppYnJtc2pxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg3ODM0MzAsImV4cCI6MjEwNDM1OTQzMH0.jH85d4G-66FNW96AX8QQhZCnNspb9smzz6Hp2TcmHrs"),
                readOrDefault("SUPABASE_STORAGE_BUCKET", "expense-receipts"),
                readPositiveLongOrDefault(
                        "SUPABASE_RECEIPT_MAX_BYTES",
                        DEFAULT_MAX_RECEIPT_SIZE_BYTES
                )
        );
    }

    public boolean isSupabaseConfigured() {
        return !supabaseUrl().isBlank() && !supabaseAnonKey().isBlank();
    }

    private static String read(String name) {
        String sysProp = System.getProperty(name);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String env = System.getenv(name);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return ENV_FILE_CACHE.getOrDefault(name, "").trim();
    }

    private static String readOrDefault(String name, String fallback) {
        String value = read(name);
        return value.isBlank() ? fallback : value;
    }

    private static long readPositiveLongOrDefault(String name, long fallback) {
        try {
            long value = Long.parseLong(read(name));
            return value > 0 ? value : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> map = new HashMap<>();
        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("expense-tracker-desktop", ".env"),
                Path.of("..", ".env")
        );
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                try {
                    List<String> lines = Files.readAllLines(candidate);
                    for (String rawLine : lines) {
                        String line = rawLine.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\""))
                                    || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            map.putIfAbsent(key, val);
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return map;
    }
}