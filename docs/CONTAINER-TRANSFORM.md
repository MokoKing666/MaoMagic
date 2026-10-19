# 容器变形转场（Container Transform）

> 点板块页的功能卡片，卡片连续长成整页；返回时原路缩回。
> 不是「缩放 + 位移 + 淡入淡出」三个动画拼出来的近似效果，而是一套共享边界运动。

实现位置：`designsystem/src/main/java/com/maohongyuan/magic/designsystem/glass/MagicSharedTransition.kt`

---

## 一、为什么转场代码要放在 designsystem

项目的导航是**手写状态机**（`MainActivity` 里的 `tab` + `openSystems[4]`），
没有 `NavHost`。转场因此不能挂在导航库里，只能在渲染层做。

但把 `SharedTransitionLayout` 直接写进 `MainActivity` 会让「哪个页面跟哪张卡片配对」
散落成字符串常量。所以配对 key、时间轴、圆角换算、玻璃面板全部收进 designsystem，
`app` 只负责「把当前路由告诉它」。

---

## 二、结构

```
MagicScaffold
└── layerBackdrop(contentBackdrop)        ← 内容层录制范围
    └── SharedTransitionLayout            ← 必须在这里面（见第四节）
        └── AnimatedContent<MagicPage>    ← 状态切换时新旧两页同时在树里
            ├── Board  ─ MagicSystemCard(sharedId = "BAZI")
            └── Feature ─ MagicContainerTransformSurface(pageBounds = "BAZI")
```

`AnimatedContent` 是关键：原来的 `when` 分支是**瞬间换内容**，
只有旧页已经退场、新页刚进场时 `sharedBounds` 才有「起点 / 终点」可插值。

---

## 三、两个共享 key，各管一件事

| key | API | 负责 |
|---|---|---|
| `magic:container:<id>` | `Modifier.sharedBounds()` | 容器矩形的位置 + 宽高连续变化 |
| `magic:title:<id>` | `Modifier.sharedBounds()`（不是 `sharedElement`） | 卡片标题 ↔ 页面大标题 |

### 为什么标题不用 `sharedElement`

`sharedElement` 的语义是「两端内容在相同约束下布局一致」，它按动画尺寸**重新布局**、
不做缩放。卡片标题 17sp、页面大标题 34sp —— 实测直接把 34sp 的字塞进 17sp 的框里，
被裁成「八」。改用 `sharedBounds + scaleToBounds(FillWidth)` 后，两端各自按目标尺寸
布局再等比缩放到动画边界，交叉淡入时视觉尺寸一致，读起来才是「同一个标题在长大」。

### 为什么不做图标共享

卡片有图标（26dp），页面大标题区没有对应物。**没有对应关系就不伪造** ——
图标跟随所在容器一起淡出，不强行拉一条不存在的运动线。

---

## 四、玻璃怎么跟着容器走

共享元素在转场期间被搬到 `SharedTransitionLayout` 的 overlay 里绘制。
只要 overlay 还在 `layerBackdrop` 子树内，玻璃表面就仍通过 `LocalBackdrop`
（屏幕锚定环境折射源）采样背景 —— 折射跟着容器一起移动，
而不是「卡片玻璃消失、页面再画一块新的」。

⚠️ 转场只叠加 `drawBackdrop`（读），**绝不新增 `layerBackdrop`（写）**。
写会与录制形成渲染死循环，最终 SIGSEGV（见 ARCHITECTURE.md 三条铁律）。

容器本体是一块 `MagicGlassSurface`，它：

1. 用与裁剪区**同一个** `MagicSmoothCornerShape` 对象 —— 玻璃边缘、裁剪区、
   内容边界永远一致，不会出现「玻璃是圆的、内容被切成方的」。
2. 进度 0→0.12 快速淡入，0.55 之后淡出。终点处完全消失，
   页面静止时不会多出一层本不该存在的全屏玻璃。
3. 不投影 —— 投影会在缩放时被放大成一片灰雾。

---

## 五、圆角为什么要在「子坐标系」里换算

共享边界用 `scaleToBounds`：子内容按**最终尺寸**布局，再整体缩放。
于是子坐标系里的圆角 `r` 在屏幕上看起来是 `r × s`（s = 当前缩放比）。

要在屏幕上呈现 `28dp × (1 − p)` 的连续收角，子坐标系里必须写：

```
r(p) = 28dp × (1 − p) / s(p)      s(p) = 0.45 + 0.55p
```

`0.45` 是来源卡片宽度占页面宽度的比例（2 列网格：(屏宽 − 44dp) / 2 / 屏宽，
手机与平板实测落在 0.44~0.47）。不换算的后果是：容器放大过程中玻璃面板的圆角
比裁剪区更圆，四个角露出底图的月牙形缺口。

---

## 六、时间轴

`MagicTransitionSpec.DurationMillis = 320`，与边界弹簧
（`StiffnessMediumLow(400) + 0.86 阻尼`，收敛约 230ms）大致同步。

早期写成 420ms 时，容器在 230ms 就已经铺满、内容却还在慢慢淡入，
观感变成「先跳成一整页，再慢慢亮起来」，而不是「卡片长成页面」。

各段淡入淡出全部用 `keyframes` 显式写 `durationMillis`：把 alpha 早早拉满/拉没，
但让动画**时长**停在 320ms —— 因为 `AnimatedContent` 是靠退出动画跑完来销毁旧页的，
提前销毁会连带取消共享元素转场。

---

## 七、几个踩过的坑

### 1. 页面先以整页尺寸闪一下

目标页第一次组合到共享边界真正激活之间会隔一段时间（页面首帧要建十几块玻璃，
debug 构建实测 100~200ms）。这段时间里共享边界尚未生效，页面会以整页尺寸画出来。

对策：`MagicContainerTransformSurface` 在进度仍为 0 时**不画页面内容**。
此刻来源卡片仍在原位完整可见，藏起来不会露出空白。

### 2. 板块页在退场时整体下跳

`LocalBackButtonVisible` 原先由 `MagicScaffold` 全局下发。钻取时它立刻变 true，
正在淡出的板块页顶部内边距也跟着涨 44dp —— 整页往下跳一下。

对策：在 `AnimatedContent` 的内容里**按页面**重新下发这个 CompositionLocal。

### 3. 组合期读 `isTransitionActive`

它是快照状态，组合期读会订阅它，导致板块页在转场开始/结束时被额外重组。
只在点击回调里读。

### 4. 返回后列表跳回顶部

原先板块页与功能页共用 `MagicScaffold` 下发的那个 `ScrollState`，
「进功能页先 scrollTo(0)」把板块页也归零了 —— 返回时落在顶部，
转场起点也就对不上。现在功能页各自持有 `ScrollState`。

---

## 八、哪些导航不走这套

只有「板块页 ↔ 功能页」这一对使用容器变形。tab 切换、设置页、许可页
一律走普通淡入淡出 —— 没有来源卡片的导航硬套卡片缩放只会显得莫名其妙。

配对 key 由 `MagicPage` 派生：`Board` 与 `Feature` 的 key 天然不同，
所以 tab 之间、功能页之间互跳都不会误配对，自动退回淡入淡出。

---

## 九、已知限制

- **预测性返回未启用**（manifest 里没有 `android:enableOnBackInvokedCallback`），
  `BackHandler` 走的是传统路径，因此不存在「预测性返回动画 + 共享元素反向动画」打架的问题。
  若将来开启，需要单独验证。
- 目标页首帧组合成本较高的机型上，点按到动画启动之间会有一次明显停顿。
  这是页面首次组合（十几块玻璃 + RuntimeShader 编译）的成本，与转场本身无关；
  release + R8 下明显更短。
- 共享元素进入 overlay 后不再参与 `contentBackdrop` 的录制，
  因此转场那 300ms 内底栏的折射看不到正在运动的页面。属可接受范围。
