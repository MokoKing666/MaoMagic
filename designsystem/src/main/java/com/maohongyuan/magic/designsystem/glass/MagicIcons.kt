package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 底部导航图标。
 *
 * 之所以自绘而不用卦符（☰☱☲☷）：八卦符号在 25dp 尺寸下全是「横线变体」，
 * 用户无法在一秒内分辨。自绘图形的**外轮廓**差异显著，转灰度也能区分。
 *
 * 四个图形各自对应语义：
 * - [FOUR_PILLARS] 四根竖柱 —— 八字四柱
 * - [SIX_YAO]      六条爻线，其中一条断开并加粗 —— 六爻变爻
 * - [PLUM_FLOWER]  五瓣梅花 —— 梅花易数
 * - [ALMANAC]      日历页 + 折角 + 日期点 —— 黄历
 *
 * 选中态用**填充**、未选中用**描边**，不只靠颜色区分（色觉障碍友好）。
 */
enum class MagicTabIcon { FOUR_PILLARS, SIX_YAO, PLUM_FLOWER, ALMANAC }

@Composable
fun MagicTabIconView(
    icon: MagicTabIcon,
    tint: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (icon) {
            MagicTabIcon.FOUR_PILLARS -> drawFourPillars(tint, selected)
            MagicTabIcon.SIX_YAO -> drawSixYao(tint, selected)
            MagicTabIcon.PLUM_FLOWER -> drawPlumFlower(tint, selected)
            MagicTabIcon.ALMANAC -> drawAlmanac(tint, selected)
        }
    }
}

// ── 八字：四根竖柱 ──────────────────────────────────────────
private fun DrawScope.drawFourPillars(tint: Color, selected: Boolean) {
    val ratios = listOf(0.60f, 0.80f, 0.68f, 0.92f)
    val barW = size.width * 0.145f
    val gap = (size.width - barW * 4) / 3f
    val radius = CornerRadius(barW / 2f)
    ratios.forEachIndexed { i, r ->
        val h = size.height * r
        val topLeft = Offset(i * (barW + gap), (size.height - h) / 2f)
        val sz = Size(barW, h)
        if (selected) {
            drawRoundRect(tint, topLeft, sz, radius)
        } else {
            drawRoundRect(tint, topLeft, sz, radius, style = Stroke(width = barW * 0.34f))
        }
    }
}

// ── 六爻：六条爻线，第二条断开（变爻）────────────────────────
private fun DrawScope.drawSixYao(tint: Color, selected: Boolean) {
    val rows = 6
    val lineH = size.height * 0.095f
    val gap = (size.height - lineH * rows) / (rows - 1)
    val radius = CornerRadius(lineH / 2f)
    val stroke = Stroke(width = lineH * 0.34f)
    val breakGap = size.width * 0.22f

    for (i in 0 until rows) {
        val y = i * (lineH + gap)
        val moving = i == 1 // 变爻：断开且加粗
        if (moving) {
            val segW = (size.width - breakGap) / 2f
            drawRoundRect(tint, Offset(0f, y), Size(segW, lineH), radius)
            drawRoundRect(tint, Offset(size.width - segW, y), Size(segW, lineH), radius)
        } else if (selected) {
            drawRoundRect(tint, Offset(0f, y), Size(size.width, lineH), radius)
        } else {
            drawRoundRect(tint, Offset(0f, y), Size(size.width, lineH), radius, style = stroke)
        }
    }
}

// ── 梅花：五瓣 ─────────────────────────────────────────────
private fun DrawScope.drawPlumFlower(tint: Color, selected: Boolean) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val petal = size.minDimension * 0.185f
    val orbit = size.minDimension * 0.285f

    for (i in 0 until 5) {
        val angle = (-90.0 + i * 72.0) * PI / 180.0
        val center = Offset(
            cx + (cos(angle) * orbit).toFloat(),
            cy + (sin(angle) * orbit).toFloat(),
        )
        if (selected) {
            drawCircle(tint, petal, center)
        } else {
            drawCircle(tint, petal, center, style = Stroke(width = petal * 0.36f))
        }
    }
    // 花心
    drawCircle(
        color = if (selected) Color.White.copy(alpha = 0.85f) else tint,
        radius = petal * 0.42f,
        center = Offset(cx, cy),
    )
}

// ── 黄历：日历页 + 折角 ────────────────────────────────────
private fun DrawScope.drawAlmanac(tint: Color, selected: Boolean) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * 0.14f)
    val bandH = h * 0.22f
    val strokeW = w * if (selected) 0.105f else 0.075f
    val stroke = Stroke(width = strokeW)

    val bodyTop = h * 0.06f
    val bodyH = h * 0.88f

    // 页面主体：两种状态都用描边，选中态只是加粗。
    //
    // 这里踩过一个坑：原实现在选中态把整页填成实心，再挖白装订带。
    // 结果「黄历」的实心面积远大于另外三个图标（四柱 / 六爻 / 梅花都是线条），
    // 在底栏里读成一个突兀的实心蓝方块，视觉重量严重失衡。
    drawRoundRect(tint, Offset(0f, bodyTop), Size(w, bodyH), corner, style = stroke)

    // 装订带：选中态填充，未选中只画一条横线 —— 用「小面积填充」表达选中，
    // 而不是「整块填充」。
    if (selected) {
        drawRoundRect(
            color = tint,
            topLeft = Offset(strokeW / 2f, bodyTop + strokeW / 2f),
            size = Size(w - strokeW, bandH - strokeW / 2f),
            cornerRadius = CornerRadius(corner.x * 0.55f),
        )
    } else {
        drawLine(
            color = tint,
            start = Offset(0f, bodyTop + bandH),
            end = Offset(w, bodyTop + bandH),
            strokeWidth = strokeW,
        )
    }

    // 日期点阵 2×2：选中态实心、未选中态描边
    val dotR = w * 0.075f
    val gridTop = bodyTop + bandH + h * 0.10f
    val colX = listOf(w * 0.32f, w * 0.68f)
    val rowY = listOf(gridTop, gridTop + h * 0.24f)
    for (y in rowY) {
        for (x in colX) {
            if (selected) {
                drawCircle(tint, dotR, Offset(x, y))
            } else {
                drawCircle(tint, dotR, Offset(x, y), style = Stroke(width = dotR * 0.55f))
            }
        }
    }
}
