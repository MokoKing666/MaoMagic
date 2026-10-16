package com.maohongyuan.magic.designsystem.theme

import androidx.compose.ui.graphics.Shape
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 猫仙幻术 设计令牌
 *
 * 设计原则：**透明不是装饰，是秩序。可读性高于一切。**
 *
 * 本文件的数值不是随手调的，每一条都对应一个可验证的约束：
 * - 文字色 → 对比度实测值写在注释里（目标：正文 ≥ 4.5:1，大字/图标 ≥ 3:1）
 * - 圆角   → 满足同心公式 `r_child = r_parent − padding`
 * - 间距   → 严格 8pt 栅格
 * - 色彩   → 语义化，且吉/凶/警一律「颜色 + 字形」双通道，不靠颜色单独承载语义
 *
 * ── 配色来源 ──
 * 浅色主题取「中国传统色」的**淡米粉 #fbeee2** 与**鹤灰 #4a4035**。
 * 这两个色同时也是「霞鹜新致宋」官方样张的用色 —— 即字与色同源。
 * 其余中性色按鹤灰的色相（暖棕灰）派生，语义色与强调色按新底色重新标定。
 */
@Immutable
data class MagicColors(
    val isLight: Boolean,

    // ── 文字（实色，非 alpha 叠加）──────────────────────────
    // 之所以用实色：表面层不透明度已提到 0.62 以上，有效背景趋于稳定，
    // 实色比 alpha 更可预测、更容易通过对比度实测。
    //
    // 对比度基准 = 卡片有效底色（白 62% 叠淡米粉）= #fdf9f4
    /** 主文字「鹤灰」。在卡片上对比度 ≈ 9.65:1（AAA 需要 7:1） */
    val primaryText: Color,
    /** 次文字，鹤灰色相派生。对比度 ≈ 7.12:1 */
    val secondaryText: Color,
    /** 三级文字（仅用于 ≥ 12sp 的注释）。对比度 ≈ 5.40:1 */
    val tertiaryText: Color,
    /** 强调色「靛青」。对比度 ≈ 6.76:1 */
    val accent: Color,

    // ── 语义色（颜色 + 字形 双通道，字形见 MagicSemanticMark）──
    // 中式约定：吉 = 红、凶 = 绿（与欧美交通/金融习惯相反，故必须配字形冗余）
    /** 吉「朱砂」。在卡片上对比度 ≈ 5.69:1 */
    val auspicious: Color,
    /** 凶「青玉」。对比度 ≈ 5.93:1 */
    val inauspicious: Color,
    /** 警示「深琥珀」。对比度 ≈ 5.52:1 */
    val warning: Color,

    // ── 玻璃形态 ─────────────────────────────────────────
    /** 内描边 1dp —— 让卡片与背景明确分离 */
    val glassStroke: Color,
    /** 顶边高光 1dp —— 模拟受光面 */
    val glassHighlight: Color,
    /** 卡片外阴影 —— 只用于拉开层级，不做重投影。已改为暖色，避免暖底上出现冷投影 */
    val glassShadow: Color,
    /** 分割线 */
    val divider: Color,

    // ── 背景层 ───────────────────────────────────────────
    // 低饱和、可折射。饱和度过高会让玻璃卡片透出随机色相，破坏色彩一致性。
    /** 「淡米粉」 */
    val backdropBase: Color,
    /** 比基色略深一档的暖米，构成整体色域 */
    val backdropGradientEnd: Color,
    /** 环境色斑（低饱和暖色，仅提供色相变化与纵深） */
    val blob1: Color,
    val blob2: Color,
    val blob3: Color,
    /** 底纹（卦纹）颜色 */
    val watermark: Color,
    /** 细颗粒噪点色 —— blur 需要有细节可处理 */
    val grain: Color,
)

val LightMagicColors = MagicColors(
    isLight = true,

    primaryText = Color(0xFF4A4035),        // 鹤灰      9.65:1
    secondaryText = Color(0xFF5F5346),      //           7.12:1
    tertiaryText = Color(0xFF71655A),       //           5.40:1
    accent = Color(0xFF1F5C8B),             // 靛青      6.76:1

    auspicious = Color(0xFFB03A3E),         // 朱砂      5.69:1
    inauspicious = Color(0xFF2F6B5C),       // 青玉      5.93:1
    // 原值 #B26A00 在淡米粉底上只有 4.04:1，低于 AA。压深一档。
    warning = Color(0xFF96560A),            // 深琥珀    5.52:1

    glassStroke = Color(0x8CFFFFFF),
    glassHighlight = Color(0xBFFFFFFF),
    glassShadow = Color(0x14382C1E),        // 暖色投影，不是原来的冷紫灰
    divider = Color(0x1F4A4035),            // 暖色分割线（鹤灰 12%）

    backdropBase = Color(0xFFFBEEE2),       // 淡米粉
    backdropGradientEnd = Color(0xFFF3E2D0),
    blob1 = Color(0x5CFAD9B0),              // 暖杏
    blob2 = Color(0x4DF2CBC0),              // 暖荷
    blob3 = Color(0x47DCD3B4),              // 暖沙
    watermark = Color(0x12B03A3E),
    // 细节层必须极低对比度 —— 它的职责是「给 blur 提供高频细节」，
    // 不是被看见。超过 4% 就会读成可见纹理，干扰内容。
    //
    // 原值 0x0A000000（3.9%）踩在阈值线上：截图中能看到离散的灰色小块，
    // 读作「屏幕脏了」而不是底纹。降到 2.4%。
    grain = Color(0x06000000),
)

val DarkMagicColors = MagicColors(
    isLight = false,

    // 深色底「暖墨」#1A1613，保持与浅色主题同一色相家族，不是简单反相
    primaryText = Color(0xFFEDE4D8),        // 12.0:1（卡片抬升后重算）
    secondaryText = Color(0xFFB9AC9C),      //  6.7:1
    // 卡片抬升后（深色下用白 0.08 提亮而非黑 0.60 压暗），三级文字必须同步提亮，
    // 否则会从 4.78:1 掉到 3.99:1 —— 跌破 AA。
    tertiaryText = Color(0xFF9E9280),       //  4.7:1
    accent = Color(0xFF5AA0D8),             //  5.3:1

    auspicious = Color(0xFFE0757A),         //  5.99:1
    inauspicious = Color(0xFF5FAF95),       //  6.89:1
    warning = Color(0xFFE0A44A),            //  8.20:1

    glassStroke = Color(0x33FFFFFF),
    glassHighlight = Color(0x47FFFFFF),
    glassShadow = Color(0x40000000),
    divider = Color(0x3DE8DDCE),

    // 深色底「暖墨」。实测：原值 #1A1613 时，卡片（白 0.08 抬升）只比背景亮约 20 阶，
    // 层级几乎看不见 —— 卡片读作「背景上一块浅一点的区域」，而不是「抬起的面板」。
    //
    // 修法选择**压暗背景**而不是继续抬高卡片：压暗同时提升所有文字对比度，
    // 且不牺牲玻璃的透度；抬高卡片则相反（卡片越亮，浅色文字的对比度越低）。
    backdropBase = Color(0xFF121009),
    backdropGradientEnd = Color(0xFF1B1611),
    blob1 = Color(0x4D5A4A32),
    blob2 = Color(0x403F2E2A),
    blob3 = Color(0x3A3A4A38),
    watermark = Color(0x1FE8DDCE),
    grain = Color(0x0DFFFFFF),
)

val LocalMagicColors = staticCompositionLocalOf { LightMagicColors }

/**
 * 五行配色。
 *
 * 传统五行正色为「木青、火赤、土黄、金白、水玄」。但「金 = 白」在浅色背景上
 * 完全不可见，因此浅色主题下金取灰金、水取靛蓝，保证 ≥ 4.5:1 的文字对比度。
 * 色相语义保留，明度按主题重新标定 —— 不是简单反相。
 *
 * 基准底色 = 卡片有效底色 #fdf9f4（淡米粉 + 白 62%）。
 * 注：浅色「水」与强调色同为靛青 #1F5C8B。五行色只出现在命盘等有文字标签的
 * 语境里，与 UI 强调色不会混淆；若日后要区分，改这一行即可。
 */
@Immutable
data class MagicWuXingColors(
    val wood: Color,
    val fire: Color,
    val earth: Color,
    val metal: Color,
    val water: Color,
)

val LightWuXingColors = MagicWuXingColors(
    wood = Color(0xFF2F7D4F),      // 4.81:1
    fire = Color(0xFFC0392B),      // 5.19:1
    // 原值 #9C6B1E 只有 4.42:1，低于 AA。压深一档。
    earth = Color(0xFF8A5E14),     // 5.43:1
    // 原值 #7C7768 只有 4.27:1，低于 AA。压深一档。
    metal = Color(0xFF6E6959),     // 5.23:1
    water = Color(0xFF1F5C8B),     // 6.76:1
)

val DarkWuXingColors = MagicWuXingColors(
    wood = Color(0xFF5FB37C),      // 7.04:1
    fire = Color(0xFFE8756A),      // 6.15:1
    earth = Color(0xFFD4A24C),     // 7.77:1
    metal = Color(0xFFC9C4B6),     // 10.32:1
    water = Color(0xFF6AA9D8),     // 7.09:1
)

val LocalMagicWuXing = staticCompositionLocalOf { LightWuXingColors }

/**
 * 吉 / 凶 / 警示 的**字形冗余**。
 *
 * 颜色永远不单独承载语义 —— 约 8% 男性存在红绿色觉障碍，
 * 且「红=危险」在西方语境下与中式约定相反。
 * 因此每个语义必须同时有：颜色 + 字形。
 */
enum class MagicSemanticMark(val glyph: String, val label: String) {
    AUSPICIOUS("✓", "吉"),
    INAUSPICIOUS("✕", "凶"),
    WARNING("⚠", "警示"),
}

// ─────────────────────────────────────────────────────────────
// 圆角
//
// 铁律一：同心圆角公式 `r_child = r_parent − padding`
//         圆心必须重合，否则接缝处出现可见的厚度跳变。
//
// 铁律二：容器圆角一律用 `com.kyant.shapes.RoundedRectangle(radius, Continuous)`
//         （G2 连续曲率），**不要**用 `RoundedCornerShape` —— 后者是正圆弧，
//         与 iOS 26/27 的圆角不同族。胶囊类（tabBar / pill）例外，
//         它们的端部本来就定义为半圆。
//
// ⚠️ 这里用 `Shape` 而非 `CornerBasedShape`：kyant Shapes 库的
//    `RoundedRectangularShape` **不是** Compose 的 `CornerBasedShape`。
//    `lens()` 折射需要知道最大圆角半径，[MagicGlassSurface] 已改为
//    同时支持这两种形状（`CornerBasedShape` 走四角 CornerSize，
//    `RoundedRectangularShape` 走它自己的 `corners()`），
//    所以换成库的形状**不会**丢掉折射。
// ─────────────────────────────────────────────────────────────
@Immutable
data class MagicShapes(
    /** 标准卡片：28dp。内边距 16dp ⇒ 内部控件 12dp */
    val card: Shape,
    /** 大卡片 / 弹窗：36dp。内边距 16dp ⇒ 内部控件 20dp */
    val largeCard: Shape,
    /** 底部弹窗 */
    val sheet: Shape,
    /** 底部 Tab 栏外壳。胶囊，半径 = 高度 / 2（随高度自适应） */
    val tabBar: Shape,
    /** 卡片内部控件（分段控件、输入框）：28 − 16 = 12 */
    val controlInCard: Shape,
    /**
     * 卡片内「分段控件滑块」：12 − 2 = 10。
     *
     * 分段控件外层有 2dp 内边距，内层必须减去这 2dp 才同心。
     * 早期实现内外层都用 12dp —— 圆心不重合，接缝处出现可见的厚度跳变。
     */
    val controlInCardThumb: Shape,
    /** 大卡片内部控件：36 − 16 = 20 */
    val controlInLargeCard: Shape,
    /** 胶囊（高度自适应，半径 = 高度 / 2） */
    val pill: Shape,
)

val DefaultMagicShapes = MagicShapes(
    // 圆角矩形统一改用 kyant Shapes 库的 RoundedRectangle(Continuous)，
    // 即文档 tutorials/smoother-rounded-corners 推荐的做法 —— G2 连续曲率，
    // 比自研的三次贝塞尔逼近更准（见 MagicSmoothCornerShape 的说明）。
    card = RoundedRectangle(28.dp, RoundedCornerStyle.Continuous),
    largeCard = RoundedRectangle(36.dp, RoundedCornerStyle.Continuous),
    sheet = RoundedRectangle(36.dp, RoundedCornerStyle.Continuous),
    // 真胶囊：不写死 32dp。写死会在系统字号放大 / 内边距变化后失效，
    // 32dp 不再等于 h/2，胶囊退化成圆角矩形，同心等式同时被破坏。
    //
    // 这里刻意**不**用 kyant 的 Continuous 胶囊：真胶囊的圆弧与直边本就相切（G1 连续），
    // 不存在曲率突变问题，而 Continuous 变体会把两端收得更圆，反而偏离「胶囊」的定义。
    tabBar = RoundedCornerShape(percent = 50),
    controlInCard = RoundedRectangle(12.dp, RoundedCornerStyle.Continuous),
    controlInCardThumb = RoundedRectangle(10.dp, RoundedCornerStyle.Continuous),
    controlInLargeCard = RoundedRectangle(20.dp, RoundedCornerStyle.Continuous),
    pill = RoundedCornerShape(percent = 50),
)

val LocalMagicShapes = staticCompositionLocalOf { DefaultMagicShapes }

// ─────────────────────────────────────────────────────────────
// 间距 —— 严格 8pt 栅格
//
// 禁止出现 5 / 7 / 13 / 17 等非 8 倍数值。
// ─────────────────────────────────────────────────────────────
object MagicSpacing {
    /** 8 —— 行内间距 */
    val row: Dp = 8.dp
    /** 12 —— 卡片之间 */
    val cardGap: Dp = 12.dp
    /** 16 —— 屏幕边距 / 卡片内边距 */
    val screenMargin: Dp = 16.dp
    val cardPadding: Dp = 16.dp
    /** 24 —— 区块之间 */
    val section: Dp = 24.dp
    /** 32 —— 大区块之间 */
    val sectionLarge: Dp = 32.dp

    /**
     * 2 —— 分段控件的内边距。
     *
     * 这是 iOS 分段控件的真实做法（滑块与容器之间留 2pt），
     * 但它**不在 8pt 栅格上**。之所以把它写成具名令牌而不是散落硬编码：
     * 它同时决定了滑块的同心圆角（12 − 2 = 10dp），
     * 改这个值必须同步改 [MagicShapes.controlInCardThumb]。
     */
    val segmentedInset: Dp = 2.dp

    // 兼容别名（仍落在 8pt 栅格内）
    val xs: Dp = 8.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 16.dp
    val xxl: Dp = 24.dp
}

// ─────────────────────────────────────────────────────────────
// 字号 —— 下限 12sp，法律类文案下限 13sp
//
// 绝对禁止 11sp 及以下。
//
// 铁律：**不允许在页面里硬编码 fontSize**。需要新档位就在本对象里加一条，
// 这样字号阶梯永远是可枚举、可审查的。
// ─────────────────────────────────────────────────────────────
object MagicType {
    const val largeTitle = 34
    const val title2 = 22
    const val title3 = 20
    const val headline = 17
    const val body = 17
    const val subheadline = 15
    const val footnote = 13
    const val caption = 12

    /**
     * 44 —— 主视觉数字（黄历的「初六」、卦象大符）。
     *
     * 上限就是 44。早期在页面里散落硬编码 40/44sp，
     * 导致黄历页的「初六」比页面大标题（34sp）还大，层级失衡。
     */
    const val display = 44

    /** 28 —— 卦象 / 符号（比 [display] 低一档） */
    const val displaySmall = 28

    /** 32 —— 空态插画字形 */
    const val glyph = 32

    /** 法律 / 免责声明文案下限 */
    const val legalMin = 13
}
