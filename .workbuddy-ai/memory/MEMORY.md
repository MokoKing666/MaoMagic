# 项目长期约定 — 猫仙幻术

## 身份
- 应用名：猫仙幻术
- 包名 / applicationId：`com.maohongyuan.magic`
- 工作区：`C:/Users/67262/WorkBuddy AI/2026-10-16-14-25-15`
- 计划文件：`C:\Users\67262\.workbuddy-ai\plans\cosmic-nebula-darwin-piP0rT7v.md`

## 不可违反的工程约束

1. **`core:model` / `core:calendar` / `core:hexagram` / `core:interpret` 是纯 JVM 模块，
   内部禁止出现任何 `android.*` 导入。** 这是单测可跑、CI 不依赖 SDK Platform 的前提。

2. **feature 模块之间禁止互相依赖。** 依赖方向严格单向。

3. **64 卦数据表禁止硬编码。** 必须由 `HexagramTable` 按京房八宫变爻规则算法生成，
   并保留 `check(size == 64)` 自检。手写 64 条记录必然出错。

4. **流体玻璃三条铁律**（违反会崩溃或效果失效）：
   - 必须调用 `Modifier.layerBackdrop(backdrop)`
   - **绝不允许在 `drawBackdrop` 之后再加 `layerBackdrop`** → 渲染死循环 → SIGSEGV
     嵌套玻璃必须改用 `exportedBackdrop`
   - 底色要先 `drawRect` 进 backdrop

5. **模型文件绝不入库、不打包进 APK。** 已在 `.gitignore` 排除
   `*.gguf` `*.litertlm` `*.tflite` `*.task` `*.onnx` `*.safetensors` `models/`。

## 技术栈基线
| 项 | 值 |
|---|---|
| AGP | 9.4.1（内建 Kotlin，**不要**加 kotlin-android 插件） |
| Gradle | 9.7.1（wrapper 已入库） |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk | 37 |
| buildToolsVersion | 36.0.0（本机唯一可用） |
| minSdk | 33（流体玻璃 lens 需 API 33+） |
| jvmTarget | 17 |
| JDK | 21 |

## 关键依赖
- `cn.6tail:lunar:1.7.7`（MIT）— 历法/干支/节气/黄历/八字/五行/十神。
  **不含**六爻装卦与梅花易数，这两块必须自研。
- `io.github.kyant0:backdrop:2.0.1`（Apache-2.0）— 流体玻璃。API 已核对源码包：
  `rememberCanvasBackdrop` / `rememberLayerBackdrop` / `Modifier.layerBackdrop` /
  `Modifier.drawBackdrop(backdrop, shape, effects, highlight, shadow, innerShadow, layerBlock, exportedBackdrop, onDrawBehind, onDrawBackdrop, onDrawSurface, onDrawFront)`；
  效果 `vibrancy()/blur()/lens()/colorFilter()/opacity()/colorControls()`，顺序强制 color filter ⇒ blur ⇒ lens。

## 用户偏好（已确认的决策）
- UI：Apple HIG + iOS 流体玻璃**全局玻璃化**，亮/暗双主题
- 解卦引擎：三档（Tier1 本地规则兜底 → Tier2 云端 OpenAI 兼容 API → Tier3 端上 LiteRT-LM），降级链必落 Tier1
- 开源协议：MIT
- 交付：GitHub 开源，一次做完 MVP
- 六爻起卦：四方式全上（铜钱/时间/数字/手动）
- 吉凶配色：中式约定，吉=红 `#D92B2B`、凶=绿 `#1F7A3D`

## 本机环境限制
- Android SDK 只有 `platforms/android-37.0` + `build-tools/36.0.0`
- **无 cmdline-tools / sdkmanager / 全局 gradle** → 无法安装其它 SDK 组件
- 无 NDK/CMake → 禁止 native 编译
- 构建命令：`./gradlew :app:assembleDebug`（首次需联网拉 lunar / backdrop）
