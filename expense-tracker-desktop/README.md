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

The current increment includes Supabase Auth login and registration, an in-memory session for the running desktop process, and a navigable application frame. Database operations, storage, OCR, and reporting are still behind interfaces and will be implemented in later increments.

## Configuration

The application reads these environment variables without logging their values:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_STORAGE_BUCKET` (defaults to `expense-receipts`)

The service-role key must never be used in this desktop application.