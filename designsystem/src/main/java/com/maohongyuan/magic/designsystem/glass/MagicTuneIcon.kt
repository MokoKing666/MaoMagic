package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * 设置图标 —— 「三条滑轨 + 圆点」。
 *
 * 为什么不画齿轮：齿轮的齿形在 22dp 这个尺寸下会糊成一圈锯齿，
 * 而「滑轨」图标在同等尺寸下依然清晰，且语义（可调参数）比齿轮更贴切 ——
 * 这个应用的设置确实以滑块为主。
 */
@Composable
fun MagicTuneIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barH = h * 0.10f
        val radius = CornerRadius(barH / 2f)
        val knobR = h * 0.15f

        // 三条滑轨的 y 位置与圆点的 x 位置（0..1）
        val rows = listOf(0.22f to 0.68f, 0.50f to 0.34f, 0.78f to 0.58f)

        rows.forEach { (yFrac, knobFrac) ->
            val y = h * yFrac
            // 滑轨
            drawRoundRect(
                color = tint.copy(alpha = 0.55f),
                topLeft = Offset(0f, y - barH / 2f),
                size = Size(w, barH),
                cornerRadius = radius,
            )
            // 圆点：挖空底再填实心，保证压在任何背景上都清晰
            val cx = w * knobFrac
            drawCircle(color = tint, radius = knobR, center = Offset(cx, y))
        }
    }
}
