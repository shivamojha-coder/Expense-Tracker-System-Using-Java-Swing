# Expense Tracker Desktop

Java Swing desktop application for recording, organizing, analyzing, and reporting personal expenses with optional receipt OCR.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- Required env: `DATABASE_URL` — Postgres connection string
- `cd expense-tracker-desktop && mvn compile` — compile the Java Swing desktop module
- `cd expense-tracker-desktop && mvn exec:java` — launch the desktop shell

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- DB: PostgreSQL + Drizzle ORM
- Validation: Zod (`zod/v4`), `drizzle-zod`
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)
- Desktop: Java 19+, Maven, Java Swing, FlatLaf
- Planned services: Supabase Auth, PostgreSQL, Storage, and RLS
- Planned OCR: Tess4J/Tesseract

## Where things live

- `expense-tracker-desktop/src/main/java/com/expensetracker/model` — domain models and enums
- `expense-tracker-desktop/src/main/java/com/expensetracker/ui` — Swing frames and application shell
- `expense-tracker-desktop/src/main/java/com/expensetracker/service` — business-service interfaces
- `expense-tracker-desktop/src/main/java/com/expensetracker/repository` — data-access interfaces
- `expense-tracker-desktop/src/main/java/com/expensetracker/supabase` — Supabase integration boundary
- `expense-tracker-desktop/pom.xml` — Java dependencies and compiler configuration

## Architecture decisions

- The desktop client remains Java/Swing; no web frontend or separate custom backend is introduced.
- Swing UI code calls service interfaces rather than constructing database or storage requests.
- Supabase configuration is read from environment variables and values are never logged.
- The first increment provides the shell and contracts without faking authenticated or persisted data.

## Product

The product will let a single authenticated user record expenses manually or with optional receipt OCR, review receipts, search and filter expenses, see dashboard analytics, and export/print reports.

## User preferences

_Populate as you build — explicit user instructions worth remembering across sessions._

## Gotchas

_Populate as you build — sharp edges, "always run X before Y" rules._

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
