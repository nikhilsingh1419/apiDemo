# Setup: Supabase + running the app

## 1. Create local env file

```bash
cp .env.local.example .env.local
```

Edit `.env.local` and set:

| Variable | Where to get it |
|----------|-----------------|
| `DATABASE_URL` | Supabase → Settings → Database → Connection string (Transaction pooler, port 6543, JDBC) |
| `DATABASE_USERNAME` | Same page — user like `postgres.YOUR_PROJECT_REF` |
| `DATABASE_PASSWORD` | Your Supabase database password |
| `JWT_SECRET` | Run `openssl rand -base64 32` |
| `GOOGLE_CLIENT_IDS` | Google Cloud → Credentials → Android + iOS OAuth client IDs (comma-separated) |

Add `?sslmode=require` to the JDBC URL if it is not already present.

## 2. Run locally

```bash
./run-local.sh
```

The script loads `.env.local`, checks required variables, and starts with profile `local`.

## 3. Fix common Supabase errors

**`tenant/user postgres.xxxx not found`**
- Supabase project is paused, deleted, or the project ref in `DATABASE_USERNAME` is wrong
- Open [Supabase Dashboard](https://supabase.com/dashboard), restore the project if paused, and copy a fresh connection string

**`password authentication failed`**
- Wrong `DATABASE_PASSWORD` — reset it in Supabase → Settings → Database

## Render

In Render → Web Service → Environment, add:

- `DATABASE_URL` — full JDBC URL with password (URL-encode special characters)
- `JWT_SECRET`
- `GOOGLE_CLIENT_IDS`

## Supabase Table Editor

After the app runs once, open Supabase → Table Editor. Tables such as `journal_entry` and `users` will appear there.
