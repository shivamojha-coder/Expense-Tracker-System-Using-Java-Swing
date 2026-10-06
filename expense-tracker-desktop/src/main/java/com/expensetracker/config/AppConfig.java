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

    /**
     * Reads settings from system properties, environment variables or a {@code .env} file (in that order).
     * The Supabase URL and key have no built-in fallback: without them {@link #isSupabaseConfigured()} is
     * false and the app offers the offline demo instead of connecting to anything.
     */
    public static AppConfig fromEnvironment() {
        return new AppConfig(
                read("SUPABASE_URL"),
                read("SUPABASE_ANON_KEY"),
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