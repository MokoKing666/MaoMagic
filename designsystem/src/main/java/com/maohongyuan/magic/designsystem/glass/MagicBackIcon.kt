package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/**
 * 返回图标 —— 左向折角箭头。
 *
 * 与 [MagicTuneIcon] 同一套画法：纯 Canvas，不依赖字体或矢量资源。
 *
 * 为什么不用「←」这个字符：它不在正文字体的子集里，会掉进系统字体，
 * 粗细和基线都跟旁边的图标对不齐。
 */
@Composable
fun MagicBackIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = h * 0.14f
        val midY = h / 2f
        // 箭头尖在左侧 22% 处，横线拉到右侧 88%
        val tipX = w * 0.22f
        val tailX = w * 0.88f
        // 折角的两条斜线
        val wing = h * 0.30f

        drawLine(
            color = tint,
            start = Offset(tipX, midY),
            end = Offset(tailX, midY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(tipX, midY),
            end = Offset(tipX + wing, midY - wing),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(tipX, midY),
            end = Offset(tipX + wing, midY + wing),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
