package com.maohongyuan.magic.designsystem.glass.icons

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 板块与术数系统的自绘图标。
 *
 * ── 绘制约定 ──
 * - **选中态填充、未选中描边**（不只靠颜色区分，色觉障碍友好）
 * - **外轮廓必须显著不同** —— 这一组图标尺寸小、数量多，
 *   若只靠内部细节区分，缩到 22dp 或转灰度后就糊成一片。
 *   因此每个图标的外形都刻意做成不同几何：圆 / 方 / 三角 / 六边 / 月牙 …
 * - 统一按 `size` 归一化，调用方只给尺寸
 *
 * ── 为什么不拆成多个文件 ──
 * 每个函数只有几行，拆开反而要在文件间跳。集中在 `icons/` 两个文件里更好找。
 */

private val STROKE_RATIO = 0.13f

private fun DrawScope.strokeW() = size.minDimension * STROKE_RATIO

/** 描边或填充 —— 按选中态决定 */
private fun DrawScope.paint(tint: Color, selected: Boolean, w: Float) =
    if (selected) null else Stroke(width = w)

// ══════════════════════════════════════════════════════════════
// 板块图标（底栏用）
// ══════════════════════════════════════════════════════════════

/** 传统 —— 罗盘：外圆 + 内十字 + 中心实点 */
internal fun DrawScope.drawBoardTraditional(tint: Color, selected: Boolean) {
    val w = strokeW()
    val r = size.minDimension * 0.42f
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(tint, r, c, style = paint(tint, selected, w) ?: Stroke(w))
    if (selected) {
        drawCircle(tint, r * 0.30f, c)
    } else {
        drawLine(tint, Offset(c.x, c.y - r * 0.6f), Offset(c.x, c.y + r * 0.6f), w, StrokeCap.Round)
        drawLine(tint, Offset(c.x - r * 0.6f, c.y), Offset(c.x + r * 0.6f, c.y), w, StrokeCap.Round)
    }
}

/** 现代 —— 扑克牌：竖矩形 + 内部菱形 */
internal fun DrawScope.drawBoardModern(tint: Color, selected: Boolean) {
    val w = strokeW()
    val cardW = size.width * 0.52f
    val cardH = size.height * 0.78f
    val tl = Offset((size.width - cardW) / 2f, (size.height - cardH) / 2f)
    val sz = Size(cardW, cardH)
    val radius = CornerRadius(cardW * 0.18f)
    if (selected) {
        drawRoundRect(tint, tl, sz, radius)
    } else {
        drawRoundRect(tint, tl, sz, radius, style = Stroke(w))
        val cx = tl.x + cardW / 2f
        val cy = tl.y + cardH / 2f
        val d = cardW * 0.26f
        val path = Path().apply {
            moveTo(cx, cy - d); lineTo(cx + d * 0.72f, cy)
            lineTo(cx, cy + d); lineTo(cx - d * 0.72f, cy); close()
        }
        drawPath(path, tint)
    }
}

/** 日常 —— 日历：矩形 + 顶部条 + 两个挂环 */
internal fun DrawScope.drawBoardDaily(tint: Color, selected: Boolean) {
    val w = strokeW()
    val bodyW = size.width * 0.78f
    val bodyH = size.height * 0.66f
    val tl = Offset((size.width - bodyW) / 2f, size.height * 0.26f)
    val radius = CornerRadius(bodyW * 0.14f)
    if (selected) {
        drawRoundRect(tint, tl, Size(bodyW, bodyH), radius)
    } else {
        drawRoundRect(tint, tl, Size(bodyW, bodyH), radius, style = Stroke(w))
        // 顶部横条：区分「日历」与「扑克牌」的关键特征
        drawLine(
            tint,
            Offset(tl.x + bodyW * 0.14f, tl.y + bodyH * 0.30f),
            Offset(tl.x + bodyW * 0.86f, tl.y + bodyH * 0.30f),
            w, StrokeCap.Round,
        )
        // 两个挂环
        val ringX = listOf(tl.x + bodyW * 0.30f, tl.x + bodyW * 0.70f)
        ringX.forEach { x ->
            drawLine(
                tint, Offset(x, size.height * 0.10f), Offset(x, tl.y + w * 0.6f),
                w, StrokeCap.Round,
            )
        }
    }
}

/** 设置 —— 三条滑轨 + 圆点 */
internal fun DrawScope.drawSettings(tint: Color, selected: Boolean) {
    val h = size.height
    val w = size.width
    val barH = h * 0.10f
    val radius = CornerRadius(barH / 2f)
    val rows = listOf(0.22f to 0.68f, 0.50f to 0.34f, 0.78f to 0.58f)
    rows.forEach { (yFrac, knobFrac) ->
        val y = h * yFrac
        drawRoundRect(
            color = tint.copy(alpha = if (selected) 1f else 0.55f),
            topLeft = Offset(0f, y - barH / 2f),
            size = Size(w, barH),
            cornerRadius = radius,
        )
        drawCircle(tint, h * 0.15f, Offset(w * knobFrac, y))
    }
}

// ══════════════════════════════════════════════════════════════
// 系统图标（卡片用，建议统一用选中态=填充）
// ══════════════════════════════════════════════════════════════

/** 八字 —— 四根不等高竖柱 */
internal fun DrawScope.drawSysBaZi(tint: Color, selected: Boolean) {
    val ratios = listOf(0.62f, 0.86f, 0.70f, 0.96f)
    val barW = size.width * 0.16f
    val gap = (size.width - barW * 4) / 3f
    val radius = CornerRadius(barW / 2f)
    ratios.forEachIndexed { i, r ->
        val h = size.height * r
        drawRoundRect(
            tint, Offset(i * (barW + gap), (size.height - h) / 2f), Size(barW, h), radius,
        )
    }
}

/** 紫微斗数 —— 星盘：圆环 + 环绕点 + 中心星 */
internal fun DrawScope.drawSysZiWei(tint: Color, selected: Boolean) {
    val w = strokeW()
    val r = size.minDimension * 0.40f
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(tint, r, c, style = Stroke(w))
    repeat(12) { i ->
        val a = Math.toRadians(i * 30.0)
        drawCircle(
            tint, r * 0.16f,
            Offset(c.x + (r * 1.0f) * Math.cos(a).toFloat(), c.y + r * Math.sin(a).toFloat()),
        )
    }
    if (selected) drawCircle(tint, r * 0.36f, c) else drawCircle(tint, r * 0.36f, c, style = Stroke(w))
}

/** 六爻 —— 三组爻线（实 / 断交替） */
internal fun DrawScope.drawSysLiuYao(tint: Color, selected: Boolean) {
    val h = size.height * 0.13f
    val gap = (size.height - h * 3) / 2f
    val radius = CornerRadius(h / 2f)
    val rows = listOf(true, false, true)
    rows.forEachIndexed { i, solid ->
        val y = i * (h + gap)
        if (solid) {
            drawRoundRect(tint, Offset(0f, y), Size(size.width, h), radius)
        } else {
            val segW = (size.width - size.width * 0.22f) / 2f
            drawRoundRect(tint, Offset(0f, y), Size(segW, h), radius)
            drawRoundRect(tint, Offset(size.width - segW, y), Size(segW, h), radius)
        }
    }
}

/** 梅花易数 —— 五瓣花 */
internal fun DrawScope.drawSysMeiHua(tint: Color, selected: Boolean) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val petalR = size.minDimension * 0.21f
    val dist = size.minDimension * 0.24f
    repeat(5) { i ->
        val a = Math.toRadians(-90.0 + i * 72.0)
        val p = Offset(c.x + dist * Math.cos(a).toFloat(), c.y + dist * Math.sin(a).toFloat())
        if (selected) drawCircle(tint, petalR, p) else drawCircle(tint, petalR, p, style = Stroke(strokeW()))
    }
    drawCircle(tint, size.minDimension * 0.10f, c)
}

/** 小六壬 —— 正六边形 + 中心点 */
internal fun DrawScope.drawSysXiaoLiuRen(tint: Color, selected: Boolean) {
    val w = strokeW()
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.42f
    val path = Path().apply {
        repeat(6) { i ->
            val a = Math.toRadians(-90.0 + i * 60.0)
            val x = c.x + r * Math.cos(a).toFloat()
            val y = c.y + r * Math.sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    if (selected) drawPath(path, tint) else drawPath(path, tint, style = Stroke(w))
    if (selected) drawCircle(Color.White.copy(alpha = 0.9f), r * 0.26f, c)
    else drawCircle(tint, r * 0.26f, c)
}

/** 大六壬 —— 四课：2×2 方格 */
internal fun DrawScope.drawSysDaLiuRen(tint: Color, selected: Boolean) {
    val w = strokeW()
    val cell = size.minDimension * 0.38f
    val gap = size.minDimension * 0.08f
    val ox = (size.width - (cell * 2 + gap)) / 2f
    val oy = (size.height - (cell * 2 + gap)) / 2f
    val radius = CornerRadius(cell * 0.18f)
    repeat(2) { r ->
        repeat(2) { col ->
            val tl = Offset(ox + col * (cell + gap), oy + r * (cell + gap))
            if (selected) drawRoundRect(tint, tl, Size(cell, cell), radius)
            else drawRoundRect(tint, tl, Size(cell, cell), radius, style = Stroke(w))
        }
    }
}

/** 奇门遁甲 —— 九宫：3×3 方格 */
internal fun DrawScope.drawSysQiMen(tint: Color, selected: Boolean) {
    val w = strokeW()
    val cell = size.minDimension * 0.245f
    val gap = size.minDimension * 0.055f
    val total = cell * 3 + gap * 2
    val ox = (size.width - total) / 2f
    val oy = (size.height - total) / 2f
    val radius = CornerRadius(cell * 0.22f)
    repeat(3) { r ->
        repeat(3) { col ->
            val tl = Offset(ox + col * (cell + gap), oy + r * (cell + gap))
            if (selected) drawRoundRect(tint, tl, Size(cell, cell), radius)
            else drawRoundRect(tint, tl, Size(cell, cell), radius, style = Stroke(w))
        }
    }
}

/** 塔罗 —— 两张交叠的牌 */
internal fun DrawScope.drawSysTarot(tint: Color, selected: Boolean) {
    val w = strokeW()
    val cw = size.width * 0.40f
    val ch = size.height * 0.72f
    val radius = CornerRadius(cw * 0.16f)
    val back = Offset(size.width * 0.10f, size.height * 0.18f)
    val front = Offset(size.width * 0.44f, size.height * 0.10f)
    if (selected) {
        drawRoundRect(tint.copy(alpha = 0.45f), back, Size(cw, ch), radius)
        drawRoundRect(tint, front, Size(cw, ch), radius)
    } else {
        drawRoundRect(tint.copy(alpha = 0.55f), back, Size(cw, ch), radius, style = Stroke(w))
        drawRoundRect(tint, front, Size(cw, ch), radius, style = Stroke(w))
    }
}

/** MBTI —— 人形：圆头 + 肩弧 */
internal fun DrawScope.drawSysMbti(tint: Color, selected: Boolean) {
    val w = strokeW()
    val headR = size.minDimension * 0.17f
    val headC = Offset(size.width / 2f, size.height * 0.32f)
    if (selected) drawCircle(tint, headR, headC) else drawCircle(tint, headR, headC, style = Stroke(w))
    // 肩：一段开口朝下的弧
    val path = Path().apply {
        val top = size.height * 0.60f
        moveTo(size.width * 0.14f, size.height * 0.92f)
        quadraticBezierTo(size.width / 2f, top - size.height * 0.14f, size.width * 0.86f, size.height * 0.92f)
    }
    drawPath(path, tint, style = if (selected) Stroke(w * 1.4f, cap = StrokeCap.Round) else Stroke(w, cap = StrokeCap.Round))
}

/** 黄历 —— 日历 + 内部日期点阵 */
internal fun DrawScope.drawSysAlmanac(tint: Color, selected: Boolean) {
    val w = strokeW()
    val bodyW = size.width * 0.80f
    val bodyH = size.height * 0.68f
    val tl = Offset((size.width - bodyW) / 2f, size.height * 0.24f)
    val radius = CornerRadius(bodyW * 0.14f)
    if (selected) {
        drawRoundRect(tint, tl, Size(bodyW, bodyH), radius)
    } else {
        drawRoundRect(tint, tl, Size(bodyW, bodyH), radius, style = Stroke(w))
    }
    // 内部 2×2 点阵 —— 与「日常」板块图标（顶部横条）区分开
    val dotR = bodyW * 0.09f
    val dotColor = if (selected) Color.White.copy(alpha = 0.9f) else tint
    repeat(2) { r ->
        repeat(2) { c2 ->
            drawCircle(
                dotColor, dotR,
                Offset(tl.x + bodyW * (0.32f + c2 * 0.36f), tl.y + bodyH * (0.42f + r * 0.30f)),
            )
        }
    }
}

/** 日运 —— 太阳：实心圆 + 八道光芒 */
internal fun DrawScope.drawSysDaily(tint: Color, selected: Boolean) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.24f
    val w = strokeW()
    drawCircle(tint, r, c, style = if (selected) Stroke(0f) else Stroke(w))
    if (selected) drawCircle(tint, r, c)
    val inner = r * 1.42f
    val outer = size.minDimension * 0.46f
    repeat(8) { i ->
        val a = Math.toRadians(i * 45.0)
        val cos = Math.cos(a).toFloat()
        val sin = Math.sin(a).toFloat()
        drawLine(
            tint,
            Offset(c.x + inner * cos, c.y + inner * sin),
            Offset(c.x + outer * cos, c.y + outer * sin),
            w, StrokeCap.Round,
        )
    }
}

/** 月运 —— 月牙（用两个圆的差集表现） */
internal fun DrawScope.drawSysMonthly(tint: Color, selected: Boolean) {
    val w = strokeW()
    val r = size.minDimension * 0.42f
    val c = Offset(size.width * 0.46f, size.height / 2f)
    val path = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                c.x - r, c.y - r, c.x + r, c.y + r,
            )
        )
        // 减去一个偏移的圆，得到月牙
        val r2 = r * 0.92f
        val c2 = Offset(c.x + r * 0.52f, c.y - r * 0.16f)
        addOval(
            androidx.compose.ui.geometry.Rect(
                c2.x - r2, c2.y - r2, c2.x + r2, c2.y + r2,
            )
        )
        fillType = PathFillType.EvenOdd
    }
    if (selected) drawPath(path, tint) else drawPath(path, tint, style = Stroke(w))
    // 一颗小星，进一步与「日运」的太阳区分
    val sx = size.width * 0.86f
    val sy = size.height * 0.24f
    val sr = size.minDimension * 0.075f
    drawCircle(tint, sr, Offset(sx, sy))
}
