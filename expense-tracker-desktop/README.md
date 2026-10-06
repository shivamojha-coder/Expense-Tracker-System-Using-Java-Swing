# Expense Tracker Desktop

A private Java Swing desktop expense tracker backed by Supabase (auth, database and receipt storage), with
receipt OCR, budgets, reports and an offline demo mode.

## Technology

- Java 19 or newer
- Maven (the included `mvnw` / `mvnw.cmd` wrapper downloads it if needed)
- Java Swing with FlatLaf
- Supabase Auth, PostgREST and Storage over plain HTTP
- Tess4J (Tesseract) for receipt OCR, JFreeChart for charts, OpenPDF for PDF export

## Setup

1. Create a Supabase project and run [`supabase/schema.sql`](supabase/schema.sql) once in the SQL editor
   (Dashboard > SQL Editor). It creates the tables, row-level security policies, the receipt storage
   bucket, budgets and the `delete_my_account()` function. It is safe to run again.
2. Put your project's values in a `.env` file next to `pom.xml` (see Configuration).

## Run

```bash
./mvnw compile exec:java        # Windows: mvnw.cmd compile exec:java
```

On Windows you can also use `run-desktop.ps1` from the repository root. To run the tests:

```bash
./mvnw test
```

## Features

- Sign in / create account (Supabase Auth), password reset, password change, account deletion
- Expenses: add, edit, delete, search and filter, receipt images with OCR suggestions
- Dashboard, reports (CSV and PDF export) and monthly budgets with alerts
- Settings: currency, date format, defaults, OCR language, budgets, data export
- Offline demo mode with sample data (no Supabase needed)

## Configuration

Settings are read from system properties, environment variables, or a `.env` file (in that order):

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_STORAGE_BUCKET` (defaults to `expense-receipts`)
- `SUPABASE_RECEIPT_MAX_BYTES` (defaults to 10 MB)
- `TESSDATA_PREFIX` (optional Tess4J/Tesseract trained-data directory; extra OCR languages need their
  `.traineddata` files there)

Only the public anon key belongs in this application. The service-role key must never be used here, and
`.env` must never be committed.
