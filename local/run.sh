#!/bin/sh
set -eu

cd "$(dirname "$0")/.."

compose() {
  docker compose -f compose.yaml "$@"
}

cleanup() {
  status=$?
  trap - EXIT
  if [ "$status" -ne 0 ]; then
    compose logs --no-color lambda >&2 || true
  fi
  if ! compose down; then
    echo "Cleanup failed; run docker compose down manually." >&2
    if [ "$status" -eq 0 ]; then status=1; fi
  fi
  exit "$status"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

compose run --build --rm process "$@"
