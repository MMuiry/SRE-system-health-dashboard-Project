#!/usr/bin/env bash
# Usage: scripts/deploy.sh <staging|production> <image-tag> [--rollback]
set -euo pipefail

ENV_NAME="$1"
TAG="$2"
MODE="${3:-deploy}"
cd "$(dirname "$0")/.."

COMPOSE_FILE="docker-compose.${ENV_NAME}.yml"
CONTAINER="app-${ENV_NAME}"
STATE_DIR=".release"
mkdir -p "$STATE_DIR"

if [ "$MODE" != "--rollback" ]; then
  CURRENT=$(docker inspect -f '{{.Config.Image}}' "$CONTAINER" 2>/dev/null | awk -F: '{print $NF}' || true)
  if [ -n "$CURRENT" ] && [ "$CURRENT" != "$TAG" ]; then
    echo "$CURRENT" > "$STATE_DIR/${ENV_NAME}.previous"
    echo "Recorded previous ${ENV_NAME} tag: $CURRENT"
  fi
fi

echo "Deploying ${TAG} to ${ENV_NAME}"
export IMAGE_TAG="$TAG"
docker compose -f "$COMPOSE_FILE" pull java-api agent
docker compose -f "$COMPOSE_FILE" up -d --build
docker compose -f "$COMPOSE_FILE" ps