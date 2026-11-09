#!/usr/bin/env bash
#
# 一键搭建本地 Paper 测试服务端（只用于本地验证插件，别拿去开服）
#
# 用法（在项目根目录）：
#   bash scripts/setup-testserver.sh
#
# 可用环境变量覆盖默认值：
#   PAPER_VERSION  Paper 版本，默认 26.2（= 游戏版本）
#   SERVER_DIR     服务端目录，默认 .tools/testserver
#   PORT           监听端口，默认 25599（避开正式服的 25565）
#   PLUGIN_JAR     要放进 plugins/ 的插件 jar，默认取 build/libs 下最新的那个
#
set -euo pipefail

# 切到项目根目录（脚本在 scripts/ 下）
cd "$(dirname "$0")/.."

PAPER_VERSION="${PAPER_VERSION:-26.2}"
SERVER_DIR="${SERVER_DIR:-.tools/testserver}"
PORT="${PORT:-25599}"

# ---------- 1. 找插件 jar ----------
if [ -z "${PLUGIN_JAR:-}" ]; then
  PLUGIN_JAR="$(ls -t build/libs/*.jar 2>/dev/null | head -n1 || true)"
fi
if [ -z "${PLUGIN_JAR:-}" ] || [ ! -f "$PLUGIN_JAR" ]; then
  echo "✗ 找不到插件 jar，请先构建：./gradlew build" >&2
  exit 1
fi
echo "==> 插件: $PLUGIN_JAR"

mkdir -p "$SERVER_DIR/plugins"

# ---------- 2. 查最新构建并下载 Paper 服务端 ----------
API="https://fill.papermc.io/v3/projects/paper/versions/${PAPER_VERSION}/builds/latest"
echo "==> 查询 Paper ${PAPER_VERSION} 最新构建 ..."
# 注意：Windows 上 curl 走 schannel 需要 --ssl-no-revoke，否则会报证书吊销检查失败
INFO="$(curl -sS --ssl-no-revoke "$API")"
URL="$(printf '%s' "$INFO" | grep -o 'https://fill-data\.papermc\.io/[^"]*' | head -n1 || true)"
if [ -z "$URL" ]; then
  echo "✗ 没能从 API 解析出下载地址，原始返回：" >&2
  printf '%s\n' "$INFO" >&2
  exit 1
fi
JAR_NAME="$(basename "$URL")"

if [ -f "$SERVER_DIR/$JAR_NAME" ]; then
  echo "==> 服务端已存在: $JAR_NAME"
else
  echo "==> 下载 $JAR_NAME ..."
  curl -sS --ssl-no-revoke -L -o "$SERVER_DIR/$JAR_NAME" "$URL"
fi

# ---------- 3. 写最小化配置 ----------
echo "eula=true" > "$SERVER_DIR/eula.txt"

if [ ! -f "$SERVER_DIR/server.properties" ]; then
  echo "==> 生成 server.properties"
  cat > "$SERVER_DIR/server.properties" <<EOF
# 本地测试用最小化配置
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

# ---------- 4. 放入插件（并清掉同名的旧版本 jar） ----------
BASE="$(basename "$PLUGIN_JAR")"
PREFIX="${BASE%%-*}"        # 例如 EscUI-1.1.0.jar -> EscUI
for f in "$SERVER_DIR/plugins/${PREFIX}"-*.jar; do
  [ -e "$f" ] || continue
  if [ "$(basename "$f")" != "$BASE" ]; then
    echo "==> 移除旧版本: $(basename "$f")"
    rm -f "$f"
  fi
done
cp "$PLUGIN_JAR" "$SERVER_DIR/plugins/"
echo "==> 已放入 plugins/: $BASE"

echo
echo "✓ 测试服务端就绪: $SERVER_DIR"
echo "  下一步运行: bash scripts/run-testserver.sh"
