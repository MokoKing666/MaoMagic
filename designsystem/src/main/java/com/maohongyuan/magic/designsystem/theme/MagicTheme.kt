package com.maohongyuan.magic.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
 * 下限硬约束：正文/注释 ≥ 13sp，图例/角标 ≥ 12sp。
 * **禁止出现 11sp 及以下** —— 法律类文案在 11sp 下不可读。
 */
private val MagicTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

/**
 * 猫仙幻术主题。
 *
 * 只负责颜色与排版令牌的下发；**不负责背景**。
 * 流体玻璃的背景层由 [com.maohongyuan.magic.designsystem.glass.MagicBackdrop] 提供，
 * 必须包在 MagicTheme 之内（因为背景要读 MagicColors）。
 *
 * 正确用法：
 * ```
 * MagicTheme {
 *     MagicBackdrop(content = { AppNavHost() }, overlay = { LiquidBottomTabs(...) })
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
            content = content,
        )
    }
}

/** 当前是否为亮色主题 */
val MagicColors.isDark: Boolean get() = !isLight
