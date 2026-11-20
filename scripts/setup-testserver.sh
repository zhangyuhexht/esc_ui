#!/usr/bin/env bash
#
# Set up a local Paper server for plugin testing. Not for production use.
#
# Run from the project root:
#   bash scripts/setup-testserver.sh
#
# Override defaults with:
#   PAPER_VERSION  Paper version, default 26.2
#   SERVER_DIR     server directory, default .tools/testserver
#   PORT           listen port, default 25599 (leaves 25565 alone)
#   PLUGIN_JAR     jar to install, default is the newest in build/libs
#
set -euo pipefail

cd "$(dirname "$0")/.."

PAPER_VERSION="${PAPER_VERSION:-26.2}"
SERVER_DIR="${SERVER_DIR:-.tools/testserver}"
PORT="${PORT:-25599}"

# ---------- 1. locate the plugin jar ----------
if [ -z "${PLUGIN_JAR:-}" ]; then
  PLUGIN_JAR="$(ls -t build/libs/*.jar 2>/dev/null | head -n1 || true)"
fi
if [ -z "${PLUGIN_JAR:-}" ] || [ ! -f "$PLUGIN_JAR" ]; then
  echo "x no plugin jar found; build first with ./gradlew build" >&2
  exit 1
fi
echo "==> plugin: $PLUGIN_JAR"

mkdir -p "$SERVER_DIR/plugins"

# ---------- 2. download the latest Paper build ----------
API="https://fill.papermc.io/v3/projects/paper/versions/${PAPER_VERSION}/builds/latest"
echo "==> looking up latest Paper ${PAPER_VERSION} build ..."
# On Windows, curl via schannel needs --ssl-no-revoke or the revocation check fails
INFO="$(curl -sS --ssl-no-revoke "$API")"
URL="$(printf '%s' "$INFO" | grep -o 'https://fill-data\.papermc\.io/[^"]*' | head -n1 || true)"
if [ -z "$URL" ]; then
  echo "x could not parse a download URL from the API response:" >&2
  printf '%s\n' "$INFO" >&2
  exit 1
fi
JAR_NAME="$(basename "$URL")"

if [ -f "$SERVER_DIR/$JAR_NAME" ]; then
  echo "==> server jar already present: $JAR_NAME"
else
  echo "==> downloading $JAR_NAME ..."
  curl -sS --ssl-no-revoke -L -o "$SERVER_DIR/$JAR_NAME" "$URL"
fi

# ---------- 3. minimal server config ----------
echo "eula=true" > "$SERVER_DIR/eula.txt"

if [ ! -f "$SERVER_DIR/server.properties" ]; then
  echo "==> writing server.properties"
  cat > "$SERVER_DIR/server.properties" <<EOF
# minimal local test config
online-mode=false
server-port=${PORT}
level-name=world
level-type=minecraft\:flat
generate-structures=false
spawn-protection=0
max-players=5
view-distance=4
simulation-distance=4
motd=EscUI Test
EOF
fi

# ---------- 4. install the plugin, removing older versions ----------
BASE="$(basename "$PLUGIN_JAR")"
PREFIX="${BASE%%-*}"        # e.g. EscUI-1.1.0.jar -> EscUI
for f in "$SERVER_DIR/plugins/${PREFIX}"-*.jar; do
  [ -e "$f" ] || continue
  if [ "$(basename "$f")" != "$BASE" ]; then
    echo "==> removing old jar: $(basename "$f")"
    rm -f "$f"
  fi
done
cp "$PLUGIN_JAR" "$SERVER_DIR/plugins/"
echo "==> installed to plugins/: $BASE"

echo
echo "Test server ready: $SERVER_DIR"
echo "Next: bash scripts/run-testserver.sh"
