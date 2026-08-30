#!/usr/bin/env bash
# Runs ON the EC2 box (as ec2-user), invoked by the GitHub Actions deploy workflow
# through SSM (AWS-RunShellScript). The workflow does the `git` sync; this script
# builds and rolls the stack, keeps the host Caddy config in step, then blocks
# until the app is healthy.
#
#   compose's `--build` needs buildx >= 0.17; Amazon Linux 2023 ships 0.12, so the
#   image is built with the legacy builder and `up` runs without `--build`.
set -euo pipefail

cd "$(dirname "$0")/.."   # repo root

DOCKER_BUILDKIT=0 docker build -t config-app -f Dockerfile .
docker compose -f config/docker-compose.aws.yml --env-file config/.env.aws up -d
docker image prune -f

# keep host Caddy config in sync with the repo (ec2-user has passwordless sudo)
if ! sudo cmp -s config/Caddyfile /etc/caddy/Caddyfile; then
  echo "Caddyfile changed — updating /etc/caddy and reloading"
  sudo cp config/Caddyfile /etc/caddy/Caddyfile
  sudo systemctl reload caddy
fi

# app needs ~35s to come up on a t2.micro; poll rather than a fixed sleep
for i in $(seq 1 24); do
  if curl -sf http://localhost:8090/actuator/health >/dev/null; then
    echo "healthy after $((i * 5))s"
    exit 0
  fi
  sleep 5
done

echo "app did not become healthy within 2 minutes" >&2
docker logs --tail 50 config-app-1 || true
exit 1
