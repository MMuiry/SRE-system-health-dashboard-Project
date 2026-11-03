#!/usr/bin/env bash
# Usage: scripts/rollback.sh <staging|production>
set -euo pipefail

ENV_NAME="$1"
cd "$(dirname "$0")/.."

PREVIOUS_FILE=".release/${ENV_NAME}.previous"
if [ ! -s "$PREVIOUS_FILE" ]; then
  echo "No previous ${ENV_NAME} tag recorded - cannot roll back"
  exit 1
fi

PREVIOUS=$(cat "$PREVIOUS_FILE")
echo "Rolling back ${ENV_NAME} to ${PREVIOUS}"
./scripts/deploy.sh "$ENV_NAME" "$PREVIOUS" --rollback

if [ "$ENV_NAME" = "production" ]; then PORT=5002; else PORT=5001; fi
./scripts/smoke_test.sh "$PORT" "$PREVIOUS"