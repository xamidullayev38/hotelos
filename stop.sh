#!/usr/bin/env bash
# Stops every HotelOS process started by ./start.sh
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -d run ]; then
  echo "no run/ dir — nothing to stop"
  exit 0
fi

for pidfile in run/*.pid; do
  [ -e "$pidfile" ] || continue
  name="$(basename "$pidfile" .pid)"
  pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    echo "stopping $name (pid $pid)..."
    kill "$pid" 2>/dev/null || true
  fi
  rm -f "$pidfile"
done

echo "all stopped."
