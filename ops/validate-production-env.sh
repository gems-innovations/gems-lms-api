#!/bin/sh
set -eu

env_file="${1:-.env}"
[ -s "$env_file" ] || { echo "Missing or empty environment file: $env_file" >&2; exit 1; }

value_of() {
  sed -n "s/^$1=//p" "$env_file" | tail -n 1 | sed "s/^['\"]//;s/['\"]$//"
}

for key in DOCKER_USERNAME ADMIN_DB_PASSWORD AUTH_DB_PASSWORD EDUCATION_DB_PASSWORD \
  REDIS_PASSWORD JWT_SECRET CORS_ALLOWED_ORIGINS FRONTEND_URL API_BASE_URL; do
  value=$(value_of "$key")
  [ -n "$value" ] || { echo "$key is required" >&2; exit 1; }
  case "$value" in CHANGE_ME*) echo "$key still contains CHANGE_ME" >&2; exit 1 ;; esac
done

jwt_secret=$(value_of JWT_SECRET)
[ "$(printf %s "$jwt_secret" | wc -c)" -ge 64 ] || {
  echo "JWT_SECRET must contain at least 64 bytes" >&2; exit 1;
}

cors_origins=$(value_of CORS_ALLOWED_ORIGINS)
frontend_url=$(value_of FRONTEND_URL)
case "$cors_origins" in *http://*) echo "CORS_ALLOWED_ORIGINS must use HTTPS" >&2; exit 1 ;; esac
case "$frontend_url" in https://*) ;; *) echo "FRONTEND_URL must use HTTPS" >&2; exit 1 ;; esac

echo "Production environment preflight passed"
