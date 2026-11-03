#!/usr/bin/env bash
# Usage: scripts/verify_monitoring.sh <environment>
set -euo pipefail

ENV_NAME="$1"

for i in $(seq 1 20); do
  RESULT=$(curl -fsSG "http://localhost:9090/api/v1/query" \
    --data-urlencode "query=up{job=\"system-health\",environment=\"${ENV_NAME}\"}" || true)
  echo "$RESULT"
  if echo "$RESULT" | grep -q '"1"\]'; then
    echo "Prometheus target for ${ENV_NAME} is UP"
    exit 0
  fi
  sleep 3
done

echo "FAIL: Prometheus target for ${ENV_NAME} is not UP"
exit 1