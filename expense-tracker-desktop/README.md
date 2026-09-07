# Expense Tracker Desktop

This module is the Java Swing foundation for the Expense Tracker System.

## Technology

- Java 19 or newer
- Maven
- Java Swing with FlatLaf
- Supabase-ready service and repository boundaries

## Run

```bash
mvn compile
mvn exec:java
```

The current increment includes Supabase Auth login and registration, an in-memory session for the running desktop process, and a navigable application frame. Expenses use the authenticated Supabase session for user-scoped create, read, update, delete, and local search/filter operations. Receipt images can be attached, previewed, replaced, or removed through Supabase Storage; Tess4J supplies editable merchant, amount, and date suggestions while manual entry remains available when OCR or upload fails.

## Configuration

The application reads these environment variables without logging their values:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_STORAGE_BUCKET` (defaults to `expense-receipts`)
- `SUPABASE_RECEIPT_MAX_BYTES` (defaults to 10 MB)
- `TESSDATA_PREFIX` (optional Tess4J/Tesseract trained-data directory)

The service-role key must never be used in this desktop application.