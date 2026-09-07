package com.expensetracker.config;

public record AppConfig(
        String supabaseUrl,
        String supabaseAnonKey,
        String storageBucket
) {
    public static AppConfig fromEnvironment() {
        return new AppConfig(
                read("SUPABASE_URL"),
                read("SUPABASE_ANON_KEY"),
                readOrDefault("SUPABASE_STORAGE_BUCKET", "expense-receipts")
        );
    }

    public boolean isSupabaseConfigured() {
        return !supabaseUrl().isBlank() && !supabaseAnonKey().isBlank();
    }

    private static String read(String name) {
        return System.getenv().getOrDefault(name, "").trim();
    }

    private static String readOrDefault(String name, String fallback) {
        String value = read(name);
        return value.isBlank() ? fallback : value;
    }
}