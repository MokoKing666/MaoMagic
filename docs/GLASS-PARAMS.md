# 玻璃参数基准 —— 全部照抄 Kyant0/AndroidLiquidGlass

> **规则：只要 Kyant0 有对应实现，参数就照抄，不自己调。**
> 自己调的结果是「看着像磨砂不像玻璃」，已经踩过一遍。

来源仓库：`Kyant0/AndroidLiquidGlass`，默认分支 **`kmp`**
（注意不是 `main`，按 main 查会 404）

```
app/src/commonMain/kotlin/com/kyant/backdrop/catalog/
├── components/LiquidButton.kt          小控件
├── components/LiquidBottomTabs.kt      底部栏（容器）
├── components/LiquidBottomTab.kt       底部栏（单个 tab）
└── utils/InteractiveHighlight.kt       按压高光
```

## 效果链

顺序固定，库内部强制：**color filter ⇒ blur ⇒ lens**

## 按尺寸分两档

| 组件 | blur | lens(height, amount) | 说明 |
|---|---|---|---|
| **LiquidButton**（小控件 48dp 高） | **2dp** | **12dp, 24dp** | 小元素模糊要轻，否则糊成一团 |
| **LiquidBottomTabs**（大栏） | **8dp** | **24dp, 24dp** | 面积大，可以厚一点 |

**关键结论：blur 跟元素尺寸走，不是全局一个值。**
我们原来全局 24dp，小控件被糊成磨砂——这是「不像玻璃」的主因。

## 遮罩色（containerColor）

```kotlin
val containerColor =
    if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)   // 近白 40%
    else              Color(0xFF121212).copy(0.4f)   // 近黑 40%
```

**深色盖近黑，不是盖白。** 深色玻璃往「更深」走。

## 小控件的 tint

```kotlin
onDrawSurface = {
    if (tint.isSpecified) {
        drawRect(tint, blendMode = BlendMode.Hue)   // 先染色相
        drawRect(tint.copy(alpha = 0.75f))          // 再盖 75%
    }
    if (surfaceColor.isSpecified) drawRect(surfaceColor)
}
```

## 按压张力

```kotlin
spring(0.5f, 300f, 0.001f)                        // 回弹
lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
maxOffset * tanh(0.05f * offset / maxOffset)      // 位移软饱和
scaleX/scaleY 按 atan2 角度各向异性拉伸
```

## 选中态

```kotlin
Highlight.Default.copy(alpha = progress)
Shadow(alpha = progress)
drawRect(Color.Black.copy(alpha = 0.03f * progress))
```

底栏选中项：高光 + 阴影 + 3% 黑的按压压暗，三者随 progress 联动。

## 我们的映射

| 我们的 Level | 对应 Kyant0 | blur | lens |
|---|---|---|---|
| `Thick` | LiquidBottomTabs | 8dp | 24 / 24 |
| `Regular` | 卡片（介于两者之间） | 8dp | 12 / 24 |
| `Thin` | LiquidButton | 2dp | 12 / 24 |
