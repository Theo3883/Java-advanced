#!/usr/bin/env bash
#
# Submit one optimization job, poll until it finishes and print the solution.
#
# Usage:
#   ./scripts/demo-optimization.sh [scenarioId]
#
# Environment:
#   BASE_URL  API base URL (default http://localhost:8080)
#   TOKEN     optional bearer token (dev profile does not need one)
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SCENARIO_ID="${1:-${SCENARIO_ID:-1}}"
TOKEN="${TOKEN:-}"

auth=()
if [[ -n "$TOKEN" ]]; then
  auth=(-H "Authorization: Bearer $TOKEN")
fi

echo "Submitting scenario $SCENARIO_ID to $BASE_URL"
job="$(curl -sS -X POST "$BASE_URL/api/v1/optimization/jobs" \
  -H 'Content-Type: application/json' "${auth[@]}" \
  -d "{\"scenarioId\":$SCENARIO_ID}")"
echo "$job" | jq .

job_id="$(echo "$job" | jq -r '.jobId // empty')"
if [[ -z "$job_id" ]]; then
  echo "Submission failed" >&2
  exit 1
fi

status=""
for _ in $(seq 1 120); do
  body="$(curl -sS "$BASE_URL/api/v1/optimization/jobs/$job_id" "${auth[@]}")"
  status="$(echo "$body" | jq -r '.status')"
  printf '\rstatus=%s progress=%s phase=%s   ' \
    "$status" "$(echo "$body" | jq -r '.progress')" "$(echo "$body" | jq -r '.phase')"
  case "$status" in
    COMPLETED)
      echo
      curl -sS "$BASE_URL/api/v1/optimization/jobs/$job_id/solution" "${auth[@]}" | jq .
      exit 0
      ;;
    FAILED|SUBMISSION_FAILED)
      echo
      echo "Job failed:" >&2
      echo "$body" | jq . >&2
      exit 1
      ;;
  esac
  sleep 0.5
done

echo
echo "Timed out waiting for job $job_id (last status $status)" >&2
exit 1
