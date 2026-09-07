package com.expensetracker.config;

public record AppConfig(
        String supabaseUrl,
        String supabaseAnonKey,
        String storageBucket,
        long maxReceiptSizeBytes
) {
    private static final long DEFAULT_MAX_RECEIPT_SIZE_BYTES = 10 * 1024 * 1024;

    public AppConfig(String supabaseUrl, String supabaseAnonKey, String storageBucket) {
        this(supabaseUrl, supabaseAnonKey, storageBucket, DEFAULT_MAX_RECEIPT_SIZE_BYTES);
    }

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
        return System.getenv().getOrDefault(name, "").trim();
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
}