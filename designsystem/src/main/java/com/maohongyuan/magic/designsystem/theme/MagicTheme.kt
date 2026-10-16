package com.maohongyuan.magic.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 排版 —— 对齐 Apple HIG 的字号阶梯。
 *
 * 字体族一律走 [MagicText]（字重自动换字面，见 [MagicFonts]）；
 * UI 标签类额外指定 [MagicSans]。
 *
 * 下限硬约束：正文/注释 ≥ 13sp，图例/角标 ≥ 12sp。
 * **禁止出现 11sp 及以下** —— 法律类文案在 11sp 下不可读。
 */
private val MagicTypography = Typography(
    displaySmall = TextStyle(fontFamily = MagicText, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = MagicText, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = MagicText, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontFamily = MagicText, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = MagicText, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = MagicText, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = MagicText, fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = MagicText, fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = MagicText, fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = MagicSans, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontFamily = MagicSans, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontFamily = MagicSans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
)

/**
 * 全局默认文字样式。
 *
 * **这一条是字体能真正生效的关键。** 项目里大量 `Text(fontSize = …, fontWeight = …)`
 * 并没有传 `style`，此时 `Text` 用的是 `LocalTextStyle`，
 * 而 `MaterialTheme` 只提供 `LocalTypography`、**不提供** `LocalTextStyle`。
 * 不显式下发的话，那些文字会退回系统默认字体（Roboto + Noto Sans CJK），
 * 装了的字体等于白装。
 *
 * 行高按中文字形上调（中文没有西文的升降部留白，1.5 倍左右才透气）。
 */
private val MagicDefaultTextStyle = TextStyle(
    fontFamily = MagicText,
    fontSize = 15.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Normal,
)

/**
 * 猫仙幻术主题。
 *
 * 只负责颜色、排版与字体族的下发；**不负责背景**。
 * 流体玻璃的背景层由 [com.maohongyuan.magic.designsystem.glass.MagicBackdrop] 提供，
 * 必须包在 MagicTheme 之内（因为背景要读 MagicColors）。
 *
 * 正确用法：
 * ```
 * MagicTheme {
 *     MagicScaffold(...)
 * }
 * ```
 */
@Composable
fun MagicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val magicColors = if (darkTheme) DarkMagicColors else LightMagicColors

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = magicColors.accent,
            onPrimary = magicColors.primaryText,
            background = magicColors.backdropBase,
            onBackground = magicColors.primaryText,
            surface = magicColors.backdropGradientEnd,
            onSurface = magicColors.primaryText,
            onSurfaceVariant = magicColors.secondaryText,
            outline = magicColors.divider,
            error = magicColors.auspicious,
        )
    } else {
        lightColorScheme(
            primary = magicColors.accent,
            onPrimary = magicColors.primaryText,
            background = magicColors.backdropBase,
            onBackground = magicColors.primaryText,
            surface = magicColors.backdropGradientEnd,
            onSurface = magicColors.primaryText,
            onSurfaceVariant = magicColors.secondaryText,
            outline = magicColors.divider,
            error = magicColors.auspicious,
        )
    }

    CompositionLocalProvider(
        LocalMagicColors provides magicColors,
        LocalMagicWuXing provides if (darkTheme) DarkWuXingColors else LightWuXingColors,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = MagicTypography,
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MagicDefaultTextStyle,
                content = content,
            )
        }
    }
}

/** 当前是否为亮色主题 */
val MagicColors.isDark: Boolean get() = !isLight
