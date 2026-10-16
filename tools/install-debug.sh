#!/usr/bin/env bash
# 构建并安装 debug 包，**并验证设备上跑的确实是刚构建的那一个**。
#
# 为什么需要这个脚本：
#   `adb install -r` 返回 `Success` 不等于安装生效。部分厂商 ROM（实测 vivo / Funtouch）
#   会拦截「同版本号覆盖安装」—— 命令成功、设备上仍是旧包，于是改了代码却看不到变化。
#   所以这里安装后必须回读设备上的 versionCode 并与构建产物比对，不一致就报错退出。
#
# 用法：
#   tools/install-debug.sh              # 构建 + 安装 + 校验
#   tools/install-debug.sh --no-build   # 跳过构建，只安装现有 APK
set -euo pipefail

PROJ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PKG="com.maohongyuan.magic.debug"
APK="$PROJ/app/build/outputs/apk/debug/app-debug.apk"
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
VENV_PY="C:/Users/67262/.workbuddy-ai/binaries/python/envs/default/Scripts/python.exe"

if [ ! -x "$ADB" ] && ! command -v "$ADB" >/dev/null 2>&1; then
  echo "找不到 adb：$ADB" >&2
  exit 1
fi

if [ "${1:-}" != "--no-build" ]; then
  echo "── 构建 ──"
  (cd "$PROJ" && ./gradlew :app:assembleDebug --no-daemon 2>&1 \
    | grep -E "^e:|^w:|FAILED|BUILD" | head -20)
fi

[ -f "$APK" ] || { echo "APK 不存在：$APK" >&2; exit 1; }

# 读取 APK 内的 versionCode（解析 AndroidManifest，避免依赖 aapt）
echo "── 构建产物的 versionCode ──"
APK_VC="$("$VENV_PY" "$PROJ/tools/apk_version.py" "$APK")"
echo "  $APK_VC"

echo "── 卸载旧包（规避同版本号拦截）──"
"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true

echo "── 安装 ──"
"$ADB" install "$APK" 2>&1 | tail -1

echo "── 校验设备上的版本 ──"
DEV_VC="$("$ADB" shell dumpsys package "$PKG" 2>/dev/null \
  | grep -m1 'versionCode=' | tr -d ' \r' | sed 's/versionCode=//; s/minSdk.*//')"

echo "  设备: $DEV_VC"
echo "  产物: $APK_VC"

if [ "$DEV_VC" != "$APK_VC" ]; then
  echo "❌ 校验失败：设备上的版本与构建产物不一致，安装没有真正生效。" >&2
  exit 1
fi
echo "✅ 校验通过：设备上跑的就是刚构建的包。"
