#!/usr/bin/env bash
#
# Submit N optimization jobs concurrently and measure throughput and latency.
#
# Usage:
#   ./scripts/benchmark-optimization.sh
#
# Environment:
#   BASE_URL      API base URL                (default http://localhost:8080)
#   SCENARIO_ID   scenario to submit          (default 1)
#   JOBS          number of jobs              (default 50)
#   CONCURRENCY   parallel submissions        (default 10)
#   TOKEN         optional bearer token
#   OUT           optional file to append the summary to
#   NOTES         free-form label stored in the summary
#
# Requires: curl, jq, awk, sort
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SCENARIO_ID="${SCENARIO_ID:-1}"
JOBS="${JOBS:-50}"
CONCURRENCY="${CONCURRENCY:-10}"
TOKEN="${TOKEN:-}"
OUT="${OUT:-}"
NOTES="${NOTES:-}"

command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }

RESULTS_DIR="$(mktemp -d)"
trap 'rm -rf "$RESULTS_DIR"' EXIT

export BASE_URL SCENARIO_ID TOKEN RESULTS_DIR

submit() {
  local index="$1"
  local auth=()
  if [[ -n "$TOKEN" ]]; then
    auth=(-H "Authorization: Bearer $TOKEN")
  fi
  curl -sS -X POST "$BASE_URL/api/v1/optimization/jobs" \
    -H 'Content-Type: application/json' "${auth[@]}" \
    -d "{\"scenarioId\":$SCENARIO_ID}" > "$RESULTS_DIR/$index.json"
}
export -f submit

echo "Submitting $JOBS jobs with concurrency $CONCURRENCY to $BASE_URL (scenario $SCENARIO_ID)"
start_ns=$(python3 -c 'import time; print(time.time_ns())')

seq 1 "$JOBS" | xargs -P "$CONCURRENCY" -I{} bash -c 'submit "$@"' _ {}

: > "$RESULTS_DIR/ids.txt"
: > "$RESULTS_DIR/submit-errors.txt"
for file in "$RESULTS_DIR"/*.json; do
  id="$(jq -r '.jobId // empty' "$file" 2>/dev/null || true)"
  if [[ -n "$id" ]]; then
    echo "$id" >> "$RESULTS_DIR/ids.txt"
  else
    cat "$file" >> "$RESULTS_DIR/submit-errors.txt"
    echo >> "$RESULTS_DIR/submit-errors.txt"
  fi
done

submitted=$(wc -l < "$RESULTS_DIR/ids.txt" | tr -d ' ')
if [[ "$submitted" -eq 0 ]]; then
  echo "No jobs were submitted. First error:" >&2
  head -50 "$RESULTS_DIR/submit-errors.txt" >&2
  exit 1
fi
echo "Submitted: $submitted/$JOBS"

mapfile -t pending < "$RESULTS_DIR/ids.txt"
: > "$RESULTS_DIR/durations.txt"
completed=0
failed=0

while [[ ${#pending[@]} -gt 0 ]]; do
  next=()
  for job_id in "${pending[@]}"; do
    auth=()
    if [[ -n "$TOKEN" ]]; then
      auth=(-H "Authorization: Bearer $TOKEN")
    fi
    body="$(curl -sS "$BASE_URL/api/v1/optimization/jobs/$job_id" "${auth[@]}")"
    status="$(jq -r '.status' <<< "$body")"
    case "$status" in
      COMPLETED)
        completed=$((completed + 1))
        jq -r '(((.completedAt // .updatedAt) | fromdateiso8601) - (.submittedAt | fromdateiso8601))' \
          <<< "$body" >> "$RESULTS_DIR/durations.txt" 2>/dev/null || true
        ;;
      FAILED|SUBMISSION_FAILED)
        failed=$((failed + 1))
        ;;
      *)
        next+=("$job_id")
        ;;
    esac
  done
  pending=("${next[@]}")
  [[ ${#pending[@]} -gt 0 ]] && sleep 0.5
done

end_ns=$(python3 -c 'import time; print(time.time_ns())')
elapsed=$(awk -v s="$start_ns" -v e="$end_ns" 'BEGIN { printf "%.3f", (e-s)/1e9 }')
throughput=$(awk -v n="$submitted" -v t="$elapsed" 'BEGIN { printf "%.2f", (t > 0 ? n/t : 0) }')

percentile() {
  local p="$1"
  sort -n "$RESULTS_DIR/durations.txt" | awk -v p="$p" '
    { values[NR] = $1 }
    END {
      if (NR == 0) { print "n/a"; exit }
      idx = int((p / 100) * NR)
      if (idx < 1) idx = 1
      if (idx > NR) idx = NR
      printf "%.3f", values[idx]
    }'
}

p50="$(percentile 50)"
p95="$(percentile 95)"
max_dur="$(sort -n "$RESULTS_DIR/durations.txt" | tail -1 || true)"

summary=$(cat <<EOF
timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)
commit: $(git rev-parse --short HEAD 2>/dev/null || echo unknown)
notes: ${NOTES}
scenario: ${SCENARIO_ID}
jobs: ${JOBS} (submitted ${submitted})
concurrency: ${CONCURRENCY}
completed: ${completed}
failed: ${failed}
wall_clock_s: ${elapsed}
throughput_jobs_per_s: ${throughput}
completion_p50_s: ${p50}
completion_p95_s: ${p95}
completion_max_s: ${max_dur:-n/a}
EOF
)

echo
echo "$summary"
if [[ -n "$OUT" ]]; then
  {
    echo '```'
    echo "$summary"
    echo '```'
    echo
  } >> "$OUT"
  echo "Appended summary to $OUT"
fi
