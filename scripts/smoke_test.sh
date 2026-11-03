#!/usr/bin/env bash
# Usage: scripts/smoke_test.sh <port> [expected-version]
set -euo pipefail

BASE_URL="http://localhost:${1}"
EXPECTED_VERSION="${2:-}"

echo "Waiting for ${BASE_URL}/health ..."
for i in $(seq 1 30); do
  if curl -fsS "${BASE_URL}/health" > /dev/null 2>&1; then
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "FAIL: app did not become healthy"
    exit 1
  fi
  sleep 2
done

for path in /health /version /metrics; do
  echo "Checking ${path}"
  curl -fsS "${BASE_URL}${path}" > /dev/null || { echo "FAIL: ${path}"; exit 1; }
done

if [ -n "$EXPECTED_VERSION" ]; then
  VERSION=$(curl -fsS "${BASE_URL}/version")
  echo "Running: ${VERSION}"
  echo "$VERSION" | grep -q "\"${EXPECTED_VERSION}\"" || { echo "FAIL: expected version ${EXPECTED_VERSION}"; exit 1; }
fi

echo "Smoke test passed for ${BASE_URL}"