#!/usr/bin/env bash
#
# 一键发布到 GitHub。
#
# 为什么需要你来跑：AI 侧的 GitHub 连接器只有读权限，
# 建仓库与建 Release 都会被 403 拒绝，所以推送这一步必须由你执行。
#
# 用法：
#   bash tools/publish-release.sh                 # 用默认仓库名
#   bash tools/publish-release.sh my-repo-name    # 自定义仓库名
#
# 前置：装好 gh 并登录一次（gh auth login），或者配好 git 凭据管理器。
set -euo pipefail

REPO_NAME="${1:-maoxian-huanzhu}"
OWNER="${GITHUB_OWNER:-MokoKing666}"
TAG="v1.1.0"
APK="app/build/outputs/apk/release/app-release.apk"

cd "$(dirname "$0")/.."

echo "── 检查前置 ──"
if [ ! -f "$APK" ]; then
  echo "找不到 $APK，先构建："
  echo "  ./gradlew :app:assembleRelease"
  exit 1
fi
echo "  APK: $APK ($(du -h "$APK" | cut -f1))"

if ! command -v gh >/dev/null 2>&1; then
  cat <<'EOF'

未检测到 gh（GitHub CLI）。

两个选择：

A) 装 gh 后重跑（推荐，能自动建仓库 + 发 Release）
   winget install GitHub.cli
   gh auth login

B) 纯 git 手动推送（只推代码，Release 需在网页上手动建）
   git remote add origin https://github.com/OWNER/REPO.git
   git push -u origin master
   git push origin v1.0.0
   然后到 https://github.com/OWNER/REPO/releases/new 建 Release，
   选 v1.0.0 标签，把上面的 APK 拖进去。

EOF
  exit 0
fi

echo "── 确认登录 ──"
gh auth status >/dev/null 2>&1 || { echo "请先执行：gh auth login"; exit 1; }
echo "  已登录"

echo "── 建仓库（已存在则跳过）──"
if gh repo view "$OWNER/$REPO_NAME" >/dev/null 2>&1; then
  echo "  $OWNER/$REPO_NAME 已存在，跳过"
else
  gh repo create "$OWNER/$REPO_NAME" \
    --public \
    --description "猫仙幻术 · 六爻八字梅花黄历 —— 完全离线的传统术数工具（Kotlin / Jetpack Compose / iOS 流体玻璃）"
  echo "  已创建 $OWNER/$REPO_NAME"
fi

echo "── 推送代码与标签 ──"
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "https://github.com/$OWNER/$REPO_NAME.git"
else
  git remote add origin "https://github.com/$OWNER/$REPO_NAME.git"
fi
git push -u origin "$(git branch --show-current)"
git push origin "$TAG"

echo "── 建 Release 并上传 APK ──"
if gh release view "$TAG" --repo "$OWNER/$REPO_NAME" >/dev/null 2>&1; then
  echo "  $TAG 已存在，改用 upload 覆盖附件"
  gh release upload "$TAG" "$APK" --repo "$OWNER/$REPO_NAME" --clobber
else
  gh release create "$TAG" "$APK" \
    --repo "$OWNER/$REPO_NAME" \
    --title "猫仙幻术 v1.0.0" \
    --notes-file tools/release-notes.md
fi

echo
echo "✅ 完成：https://github.com/$OWNER/$REPO_NAME/releases/tag/$TAG"
