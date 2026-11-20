#!/usr/bin/env bash
#
# Boot the test server once to check that the plugin loads cleanly.
# Exits 0 on success, 1 on failure — usable from CI or a pre-commit hook.
#
# Run from the project root:
#   bash scripts/run-testserver.sh
#
# Override defaults with:
#   SERVER_DIR     server directory, default .tools/testserver
#   TIMEOUT_SECS   startup timeout in seconds, default 180
#
set -uo pipefail

cd "$(dirname "$0")/.."

SERVER_DIR="${SERVER_DIR:-.tools/testserver}"
TIMEOUT_SECS="${TIMEOUT_SECS:-180}"

if [ ! -d "$SERVER_DIR" ]; then
  echo "x no test server found; run: bash scripts/setup-testserver.sh" >&2
  exit 1
fi

# Absolute path, so LOG stays valid after cd
SERVER_DIR="$(cd "$SERVER_DIR" && pwd)"
LOG="$SERVER_DIR/server.log"

# ---------- pick a Java (Paper 26.x wants Java 25) ----------
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
elif [ -d "$HOME/.jdks" ]; then
  # IDEs often install JDKs under ~/.jdks; take the newest one
  JAVA="$(ls -t "$HOME"/.jdks/*/bin/java 2>/dev/null | head -n1 || true)"
  JAVA="${JAVA:-java}"
else
  JAVA="java"
fi
echo "==> Java: $JAVA"

cd "$SERVER_DIR"
JAR="$(ls -t paper-*.jar 2>/dev/null | head -n1 || true)"
if [ -z "$JAR" ]; then
  echo "x no paper-*.jar found; run: bash scripts/setup-testserver.sh" >&2
  exit 1
fi
echo "==> server: $JAR"

rm -f "$LOG"

# Start the server, wait for "Done (", then send stop
( for _ in $(seq 1 $((TIMEOUT_SECS / 3))); do
    grep -q "Done (" "$LOG" 2>/dev/null && break
    sleep 3
  done
  sleep 2
  echo "stop"
  sleep 6
) | "$JAVA" -Xms512M -Xmx1500M -jar "$JAR" --nogui > "$LOG" 2>&1

echo
echo "----- key log lines -----"
grep -nE "Loading server plugin|Enabling|Disabling|Ambiguous|ERROR|SEVERE|Exception|Done \(" "$LOG" || true

echo
# Only inspect lines up to "Done (" — shutdown-time classloader errors from
# unrelated plugins are noise for a startup check.
STARTUP="$(sed -n '1,/Done (/p' "$LOG")"
if grep -q "Done (" "$LOG" && ! printf '%s\n' "$STARTUP" | grep -qE "SEVERE|Exception|Ambiguous"; then
  echo "RESULT = PASS"
  exit 0
else
  echo "RESULT = FAIL (see $LOG)"
  exit 1
fi
