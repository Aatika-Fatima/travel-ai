#!/usr/bin/env bash
# Fills config/.env.aws with real values pulled from the repo root's .env,
# using config/.env.aws.example as the list of keys actually needed.
#
# Run this LOCALLY, where .env already has real secrets in it — not on the
# EC2 box. It never touches the network; it only reads .env and writes
# config/.env.aws, both of which are gitignored.
set -euo pipefail

cd "$(dirname "$0")/.."  # repo root, regardless of where this is invoked from

ENV_FILE=".env"
TEMPLATE="config/.env.aws.example"
OUT="config/.env.aws"

[ -f "$ENV_FILE" ] || { echo "Missing $ENV_FILE — nothing to pull values from." >&2; exit 1; }
[ -f "$TEMPLATE" ] || { echo "Missing $TEMPLATE." >&2; exit 1; }

: > "$OUT"
missing=()

while IFS= read -r line; do
  if [[ "$line" =~ ^([A-Z_][A-Z0-9_]*)= ]]; then
    key="${BASH_REMATCH[1]}"
    value="$(grep -E "^${key}=" "$ENV_FILE" | head -n1 | cut -d'=' -f2-)"
    if [ -z "$value" ]; then
      echo "$line" >> "$OUT"   # keep the template's placeholder
      missing+=("$key")
    else
      echo "${key}=${value}" >> "$OUT"
    fi
  else
    echo "$line" >> "$OUT"     # blank lines etc., preserved as-is
  fi
done < "$TEMPLATE"

chmod 600 "$OUT"

echo "Wrote $OUT"
if [ "${#missing[@]}" -gt 0 ]; then
  echo "No value found in $ENV_FILE for these — fill them in by hand:" >&2
  printf '  %s\n' "${missing[@]}" >&2
fi
