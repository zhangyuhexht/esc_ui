#!/usr/bin/env bash
#
# 本地跑一遍测试服务端，验证插件能否正常加载。
# 通过返回 0，失败返回 1 —— 方便接到 CI 或 pre-commit 里。
#
# 用法（在项目根目录）：
#   bash scripts/run-testserver.sh
#
# 可用环境变量覆盖默认值：
#   SERVER_DIR     服务端目录，默认 .tools/testserver
#   TIMEOUT_SECS   等待启动的上限秒数，默认 180
#
set -uo pipefail

cd "$(dirname "$0")/.."

SERVER_DIR="${SERVER_DIR:-.tools/testserver}"
TIMEOUT_SECS="${TIMEOUT_SECS:-180}"

if [ ! -d "$SERVER_DIR" ]; then
  echo "✗ 测试服务端还不存在，请先运行: bash scripts/setup-testserver.sh" >&2
  exit 1
fi

# 转成绝对路径，后面 cd 进去之后 LOG 仍然有效
SERVER_DIR="$(cd "$SERVER_DIR" && pwd)"
LOG="$SERVER_DIR/server.log"

# ---------- 选一个 Java（Paper 26.x 需要 Java 25） ----------
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
elif [ -d "$HOME/.jdks" ]; then
  # 常见 IDE 会把 JDK 装在 ~/.jdks 下，取最近安装的那个
  JAVA="$(ls -t "$HOME"/.jdks/*/bin/java 2>/dev/null | head -n1 || true)"
  JAVA="${JAVA:-java}"
else
  JAVA="java"
fi
echo "==> 使用 Java: $JAVA"

cd "$SERVER_DIR"
JAR="$(ls -t paper-*.jar 2>/dev/null | head -n1 || true)"
if [ -z "$JAR" ]; then
  echo "✗ 没找到 paper-*.jar，请先运行: bash scripts/setup-testserver.sh" >&2
  exit 1
fi
echo "==> 服务端: $JAR"

rm -f "$LOG"

# 启动服务端；等出现 "Done (" 后，再发 stop 让它自己退出
( for _ in $(seq 1 $((TIMEOUT_SECS / 3))); do
    grep -q "Done (" "$LOG" 2>/dev/null && break
    sleep 3
  done
  sleep 2
  echo "stop"
  sleep 6
) | "$JAVA" -Xms512M -Xmx1500M -jar "$JAR" --nogui > "$LOG" 2>&1

echo
echo "----- 关键日志 -----"
grep -nE "Loading server plugin|Enabling|Disabling|Ambiguous|ERROR|SEVERE|Exception|Done \(" "$LOG" || true

echo
# 判定：启动完成 + 启动阶段没有致命异常
# 只看 "Done (" 之前的日志 —— 关服（stop）阶段个别插件的类加载器报错属于无害噪音
STARTUP="$(sed -n '1,/Done (/p' "$LOG")"
if grep -q "Done (" "$LOG" && ! printf '%s\n' "$STARTUP" | grep -qE "SEVERE|Exception|Ambiguous"; then
  echo "RESULT = PASS"
  exit 0
else
  echo "RESULT = FAIL （详见 $LOG）"
  exit 1
fi
