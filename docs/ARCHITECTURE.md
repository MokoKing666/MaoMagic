# 架构说明

本文档说明「猫仙幻术」的工程结构、关键设计决策与不可违反的约束。

---

## 一、模块结构

```
MaoXianHuanShu/
├── app/                    Android 外壳、导航、DI 装配
├── core/
│   ├── model/              纯 JVM · 领域模型（零依赖）
│   ├── calendar/           纯 JVM · 历法 + 八字 + 黄历
│   ├── hexagram/           纯 JVM · 64卦 + 六爻装卦 + 梅花   ★技术核心
│   ├── interpret/          纯 JVM · 解卦引擎抽象 + Tier1 规则引擎
│   ├── ai/                 Android · Tier2 云端 + Tier3 LiteRT-LM
│   └── data/               Android · DataStore 设置 / 历史 / Key 加密
├── designsystem/           Android · Apple HIG + 流体玻璃设计系统
└── feature/
    ├── bazi/  liuyao/  meihua/  almanac/
```

### 依赖方向（单向）

```
:app ──> :feature:* ──> :core:interpret ──> :core:hexagram ──> :core:model
                    │                     └─> :core:calendar ──> :core:model
                    └─> :designsystem
:core:ai ──> :core:interpret / :core:model
:core:data ──> :core:model
```

- feature 模块之间**禁止**互相依赖
- 依赖只能向下，禁止反向

---

## 二、决策 1：算法层做成纯 JVM 模块

`:core:model`、`:core:calendar`、`:core:hexagram`、`:core:interpret` 是 `java-library` 模块，**内部禁止出现任何 `android.*` 导入**。

### 为什么

| 收益 | 说明 |
|---|---|
| 单测快 | 纯 JUnit，无需设备/模拟器，秒级跑完 |
| CI 稳 | 不依赖 Android SDK Platform。托管 runner 即使没有 `platforms;android-37`，算法测试也必定能跑 |
| 可复用 | 未来可包装成 CLI 工具或桌面端，无需重写算法 |
| 边界清晰 | 算法与 UI 彻底解耦，改 UI 不会碰坏算法 |

### 代价

需要在 Android 与纯 JVM 之间手动做数据转换（例如 `LocalDate` ↔ 时间戳）。这个代价是可接受的。

---

## 三、决策 2：64 卦表由算法生成，不硬编码

### 问题

六爻装卦需要 64 卦的宫位、卦序、世爻、应爻。手写 64 条记录：

- 极易出错（且错误很难被发现）
- 无法验证
- 维护成本高

### 方案

按**京房八宫变爻规则**在 `HexagramTable` 初始化时生成全表：

| 序位 | 相对本宫卦的翻爻集合 | 世爻位 |
|---|---|---|
| 本宫(纯卦) | `{}` | 6 |
| 一世 | `{1}` | 1 |
| 二世 | `{1,2}` | 2 |
| 三世 | `{1,2,3}` | 3 |
| 四世 | `{1,2,3,4}` | 4 |
| 五世 | `{1,2,3,4,5}` | 5 |
| 游魂 | `{1,2,3,5}` | 4 |
| 归魂 | `{5}` | 3 |

应爻位由统一公式导出：`((世爻 - 1 + 3) mod 6) + 1`

```kotlin
object HexagramTable {
    private val byCode = HashMap<Int, HexagramMeta>(64)

    init {
        for (palace in Palace.entries) {
            val pureCode = (palace.trigram.bits shl 3) or palace.trigram.bits
            for (seq in PalaceSequence.entries) {
                var code = pureCode
                for (i in FLIP_SETS.getValue(seq)) code = code xor (1 shl (i - 1))
                val shi = seq.shiPos
                byCode[code] = HexagramMeta(code, palace, seq, shi, ((shi - 1 + 3) % 6) + 1)
            }
        }
        check(byCode.size == 64) { "64卦生成异常：${byCode.size}" }   // 自检
    }
}
```

`check(size == 64)` 是关键：如果变爻规则写错导致卦位重叠，构建/首次访问时立即崩溃，而不是静默产生错误结果。

卦名与卦辞放 JSON 资源，与算法解耦。

---

## 四、决策 3：解卦引擎三档分层

### 设计目标

**离线永远可用。** 用户不配 Key、不下模型、断网，也必须能解卦。

### 抽象

```kotlin
interface DivinationInterpreter {
    val tier: EngineTier          // RULE / CLOUD / LOCAL_MODEL
    val id: String
    suspend fun isAvailable(): Boolean
    fun interpret(request: InterpretationRequest): Flow<Interpretation>
}
```

### 降级链

```
Tier3 端上模型 ──失败──> Tier2 云端 AI ──失败──> Tier1 规则引擎（必定可用）
```

`InterpreterResolver` 按用户设置的 `EngineMode` 排出候选链，逐个 `isAvailable()` + `try`，任何一个环节抛异常都继续往下走，**最终无条件落到 Tier1**。

Tier1 是纯规则引擎（五行生克 + 卦辞库 + 判定规则），零网络、零模型依赖，因此**不存在「解不出来」的状态**。

### 引擎模式

| 模式 | 候选链 |
|---|---|
| `AUTO` | local → cloud → rule |
| `CLOUD_FIRST` | cloud → local → rule |
| `LOCAL_FIRST` | local → cloud → rule |
| `OFFLINE_ONLY` | rule（仅本地规则） |

---

## 五、决策 4：全局流体玻璃的「唯一根 backdrop」

### 前提

流体玻璃本质是「把背景复制一份，加效果后绘制到前景」。**纯色背景上做玻璃等于没做** —— 玻璃与实色卡片视觉无差别。

因此全局玻璃化必须建立**三层结构**：

```
Layer 3  内容层    页面内容（文字、卦盘、列表）—— 透明背景
Layer 2  玻璃层    MagicGlassSurface 包裹的卡片/按钮/栏/弹窗
Layer 1  背景层    MagicBackdrop —— 渐变 + 色斑 + 底纹，唯一折射源
```

### 唯一根 backdrop 原则

全 App 只创建一个根 `LayerBackdrop`（由 `MagicBackdrop` 提供，经 `CompositionLocal` 下发）。所有玻璃表面共享它。

原因：每个 backdrop 都是一次全屏纹理拷贝 + RenderEffect 链。多层嵌套会让性能呈指数级崩塌，并显著提高崩溃概率。

### 三条铁律

| # | 规则 | 违反后果 |
|---|---|---|
| 1 | 必须调用 `Modifier.layerBackdrop(backdrop)` | 效果完全不生效（官方 FAQ 第一坑） |
| 2 | **绝不允许在 `drawBackdrop` 之后再加 `layerBackdrop`** | 渲染死循环 → `Fatal signal 11 (SIGSEGV) in RenderThread`。嵌套玻璃必须改用 `exportedBackdrop` 参数（它跳过内容绘制） |
| 3 | 底色要先 `drawRect` 进 backdrop | 玻璃边缘出现透明像素，效果残缺 |

### 性能约束

| 约束 | 原因 |
|---|---|
| 禁止 `LazyColumn` 逐项 `drawBackdrop` | 每项都要复制背景 + 跑 RenderEffect，滚动严重掉帧。列表整体包一层 |
| 玻璃叠玻璃 ≤ 2 层 | 每层一次全屏拷贝 + shader 采样 |
| blur ≤ 24dp，lens ≤ 48dp | 采样开销随半径超线性增长 |
| 动画期间降级到 `THIN` | 滚动/转场时临时关闭 lens |
| 提供 `glassMode` 开关 | `FULL` / `REDUCED`（仅 blur）/ `OFF`（半透明实色） |
| 尊重系统无障碍设置 | 读取「减少动态效果」「降低透明度」自动降级 |

### 可读性

信息密度高的页面（八字四柱表、六爻卦盘）使用 `THIN` 档 + 高 alpha 表层色。正文对比度需 ≥ 4.5:1。

### 转场与玻璃的交界

卡片 → 页面的容器变形转场用 Compose 官方共享元素 API
（`SharedTransitionLayout` / `sharedBounds`），共享元素在转场期间被搬到
`SharedTransitionLayout` 的 overlay 里绘制。

**这个 overlay 必须留在 `layerBackdrop` 子树内部** —— 否则玻璃表面拿不到
`LocalBackdrop`，动画途中玻璃会失效、落位后又突然出现。
转场只叠加 `drawBackdrop`（读），绝不新增 `layerBackdrop`（写）。

细节见 [CONTAINER-TRANSFORM.md](CONTAINER-TRANSFORM.md)。

---

## 六、版本与构建

| 项 | 值 |
|---|---|
| JDK | 21 |
| Gradle | 9.7.1 |
| AGP | 9.4.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk | 37 |
| buildToolsVersion | 36.0.0 |
| minSdk | 33（Android 13） |
| jvmTarget | 17 |

### AGP 9 内建 Kotlin

AGP 9.0 起内建 Kotlin 支持并默认启用，因此：

- Android 模块**不再应用** `org.jetbrains.kotlin.android` 插件
- `jvmTarget` 默认取 `android.compileOptions.targetCompatibility`，无需显式声明
- Compose 编译器插件（`org.jetbrains.kotlin.plugin.compose`）与 serialization 插件仍需正常应用
- 纯 JVM 模块仍使用 `org.jetbrains.kotlin.jvm`

### minSdk 为什么是 33

流体玻璃的效果本质是 `RenderEffect`，仅 Android 12（API 31）+ 生效；其中透镜折射、色散、深度效果依赖 AGSL `RuntimeShader`，需要 Android 13（API 33）+。

选择 minSdk 33 以换取全效果可用，代价是放弃 Android 8~12 存量设备。

---

## 七、第三方依赖

| 组件 | 用途 | 许可 |
|---|---|---|
| `cn.6tail:lunar:1.7.7` | 历法内核（公历/农历/干支/节气/黄历/八字/五行/十神） | MIT |
| `io.github.kyant0:backdrop:2.0.1` | 流体玻璃效果引擎 | Apache-2.0 |
| AndroidX / Compose / Kotlin / OkHttp | 基础设施 | Apache-2.0 |

### lunar 不覆盖的部分

`lunar` 库覆盖历法与八字，但**不包含**：

- 六爻装卦（纳甲、六亲、世应、六神、伏神、变卦、互卦、空亡）
- 梅花易数（体用、互卦、变卦、五行生克）

这两块必须自研，位于 `:core:hexagram`，是本项目技术难度最高、也最需要测试覆盖的部分。

### lunar 线程安全

每次计算新建 `Lunar` / `Solar` 实例，封装为无状态纯函数，不共享可变状态。

---

## 八、已知风险

| 风险 | 缓解 |
|---|---|
| 六爻算法准确性 | 算法生成 64 卦表 + `check(size==64)` 自检 + 经典卦例黄金测试 |
| backdrop 渲染死循环崩溃 | 三条铁律 + code review 检查 `layerBackdrop` 是否出现在 `drawBackdrop` 之后 |
| 全局玻璃化性能 | 唯一根 backdrop + 列表整体玻璃化 + `glassMode` 降级开关 |
| backdrop 是 CMP 产物，可能与 androidx.compose BOM 冲突 | ✅ **已实测排除**，见下 |
| `lens()` 静默不生效 | `shape` 必须是 `CornerBasedShape`；`height ≤ shape.minCornerRadius` |
| LiteRT-LM 坐标/API 变动 | Tier3 独立模块 + `isAvailable()` 运行时探测 + 编译期可选，保证不配模型也能构建 |
| 端上模型设备兼容 | 仅 arm64；检测 RAM；模型体积与内存需求在 UI 中明示；默认不启用 |

### 依赖冲突核验命令

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath \
  | grep -iE "backdrop|jetbrains\.compose"
```

#### 核验结论（2026-10-16 实测）

依赖树中确实会出现 `org.jetbrains.compose.*:1.12.0`，但**经确认它们只是元数据重定向，不产生重复类**：

| 坐标 | 缓存中的实际文件 | 性质 |
|---|---|---|
| `org.jetbrains.compose.foundation:foundation:1.12.0` | 仅 `.pom` + `.module`，**无 `.aar` / `.jar`** | 纯元数据别名 |
| `org.jetbrains.compose.ui:ui:1.12.0` | 仅 `.pom` + `.module`，**无 `.aar` / `.jar`** | 纯元数据别名 |
| `androidx.compose.foundation:foundation-android:1.12.1` | 含 `foundation.aar` | 真实实现（来自 BOM 2026.09.00） |

佐证：`checkDebugDuplicateClasses` 通过、dex 打包通过、APK 构建成功。

**结论：无需 `resolutionStrategy` 或 `exclude` 干预。** 若未来 CMP 改为发布真实 AAR，需重新核验。

---

## 九、测试策略

| 层次 | 方式 | 依赖设备 |
|---|---|---|
| 算法内核（纯 JVM 模块） | 纯 JUnit 黄金测试 | ❌ 不需要 |
| Android 模块 | 仪器测试（后续补充） | ✅ 需要 |
| UI | 真机走查 + Compose UI 测试（后续补充） | ✅ 需要 |

**黄金测试**是项目正确性的基石，覆盖：八宫卦序全表、世应全表、纳甲全表、伏神经典卦例、六旬空亡全表、边界日期。
