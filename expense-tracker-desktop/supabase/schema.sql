-- Expense Tracker Desktop — full Supabase setup.
--
-- Run this once, in order, against a fresh Supabase project via
-- Dashboard > SQL Editor > New query. It creates everything the Java
-- client in this repo talks to: the "categories" and "expenses" tables
-- (see SupabaseCategoryRepository / SupabaseExpenseRepository) and the
-- "expense-receipts" storage bucket (see SupabaseStorageService).
--
-- Auth itself needs no SQL: Supabase Auth manages auth.users internally.
-- Just make sure email sign-ups are enabled (Auth > Providers > Email),
-- which is on by default. If "Confirm email" is also on (the default),
-- new accounts must confirm via email before they get a session — the
-- desktop app already handles that case (see MainWindow.runRegisterOperation).

-- ============================================================
-- 1. Extensions
-- ============================================================
-- gen_random_uuid() ships from pgcrypto; Supabase usually has it enabled
-- already, but this is safe to re-run.
create extension if not exists pgcrypto;

-- ============================================================
-- 2. categories
-- ============================================================
-- Shared reference data: every signed-in user reads the same list.
-- The app never lets a user create/edit/delete categories, so there is
-- no owner column and no write policies.
create table if not exists public.categories (
    id uuid primary key default gen_random_uuid(),
    name text not null
);

alter table public.categories enable row level security;

drop policy if exists "Authenticated users can read categories" on public.categories;
create policy "Authenticated users can read categories"
    on public.categories for select
    to authenticated
    using (true);

insert into public.categories (name)
select name from (values
    ('Food & Dining'),
    ('Transportation & Travel'),
    ('Shopping'),
    ('Bills & Utilities'),
    ('Entertainment'),
    ('Healthcare'),
    ('Other')
) as seed(name)
where not exists (select 1 from public.categories);

-- ============================================================
-- 3. expenses
-- ============================================================
-- One row per expense, owned by the authenticated user who created it.
-- Column names/types line up 1:1 with the fields read/written in
-- SupabaseExpenseRepository (toPayload / fromJson).
create table if not exists public.expenses (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    category_id uuid not null references public.categories(id),
    merchant text not null,
    amount numeric(12, 2) not null check (amount > 0),
    expense_date date not null,
    payment_method text not null,
    description text,
    receipt_path text,
    receipt_status text not null default 'NO_RECEIPT',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists expenses_user_id_idx on public.expenses (user_id);

alter table public.expenses enable row level security;

-- RLS is the authoritative ownership boundary (the Java repository layer
-- also filters by user_id client-side, but only as defense in depth).
drop policy if exists "Users can view their own expenses" on public.expenses;
create policy "Users can view their own expenses"
    on public.expenses for select
    to authenticated
    using (auth.uid() = user_id);

drop policy if exists "Users can insert their own expenses" on public.expenses;
create policy "Users can insert their own expenses"
    on public.expenses for insert
    to authenticated
    with check (auth.uid() = user_id);

drop policy if exists "Users can update their own expenses" on public.expenses;
create policy "Users can update their own expenses"
    on public.expenses for update
    to authenticated
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "Users can delete their own expenses" on public.expenses;
create policy "Users can delete their own expenses"
    on public.expenses for delete
    to authenticated
    using (auth.uid() = user_id);

-- Keep updated_at current on every row change.
create or replace function public.set_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

drop trigger if exists expenses_set_updated_at on public.expenses;
create trigger expenses_set_updated_at
    before update on public.expenses
    for each row
    execute function public.set_updated_at();

-- ============================================================
-- 4. Storage: expense-receipts bucket
-- ============================================================
-- Private bucket. Objects are stored at "{userId}/{expenseId}/{uuid}.ext"
-- (see SupabaseStorageService.uploadReceipt), so ownership is enforced by
-- checking the first path segment against auth.uid().
insert into storage.buckets (id, name, public)
values ('expense-receipts', 'expense-receipts', false)
on conflict (id) do nothing;

drop policy if exists "Users can upload their own receipts" on storage.objects;
create policy "Users can upload their own receipts"
    on storage.objects for insert
    to authenticated
    with check (
        bucket_id = 'expense-receipts'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

drop policy if exists "Users can view their own receipts" on storage.objects;
create policy "Users can view their own receipts"
    on storage.objects for select
    to authenticated
    using (
        bucket_id = 'expense-receipts'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

drop policy if exists "Users can delete their own receipts" on storage.objects;
create policy "Users can delete their own receipts"
    on storage.objects for delete
    to authenticated
    using (
        bucket_id = 'expense-receipts'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- ============================================================
-- Done. Verify with:
--   select * from public.categories;
--   select * from storage.buckets where id = 'expense-receipts';
-- ============================================================
