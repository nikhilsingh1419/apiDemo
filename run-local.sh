#!/bin/bash

set -euo pipefail

ENV_FILE=".env.local"

echo "🚀 Starting application with local profile..."

if [[ ! -f "$ENV_FILE" ]]; then
  echo "❌ Missing $ENV_FILE"
  echo "   Copy .env.local.example to .env.local and fill in your Supabase + auth values:"
  echo "   cp .env.local.example .env.local"
  exit 1
fi

# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

missing=()
[[ -z "${DATABASE_URL:-}" ]] && missing+=("DATABASE_URL")
[[ -z "${DATABASE_USERNAME:-}" ]] && missing+=("DATABASE_USERNAME")
[[ -z "${DATABASE_PASSWORD:-}" ]] && missing+=("DATABASE_PASSWORD")
[[ -z "${JWT_SECRET:-}" ]] && missing+=("JWT_SECRET")
[[ -z "${GOOGLE_CLIENT_IDS:-}" ]] && missing+=("GOOGLE_CLIENT_IDS")

if ((${#missing[@]} > 0)); then
  echo "❌ Missing required values in $ENV_FILE:"
  printf '   - %s\n' "${missing[@]}"
  exit 1
fi

export SPRING_PROFILES_ACTIVE=local
export DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD JWT_SECRET GOOGLE_CLIENT_IDS

echo "📊 Using Supabase database from $ENV_FILE"
mvn spring-boot:run
