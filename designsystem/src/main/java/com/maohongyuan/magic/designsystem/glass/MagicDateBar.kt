package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType
import java.time.LocalDate

/** 日期粒度：按日（黄历/日运）或按月（月运） */
enum class DateGranularity { DAY, MONTH }

/**
 * 日期条：前后翻 + 点开输入任意日期。
 *
 * ── 为什么自己画而不用 Material3 的 DatePicker ──
 * Material 的 DatePicker 是一整块 Material 语言的弹窗（容器、字体、配色、
 * 按钮形状全是 Material），塞进流体玻璃界面里会「两张皮」。
 * 本项目自绘图标、自绘滑块都是同一个理由。
 *
 * ── 为什么用 java.time.LocalDate 而不是手写 ±1 天 ──
 * 跨月、跨年、闰年（2 月 29 日）这些边界手写必错。
 * `LocalDate.plusDays/plusMonths` 是 JDK 的成熟实现，minSdk 33 原生可用，
 * 不需要 desugaring。**日期算术不要自己造。**
 */
@Composable
fun MagicDateBar(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    granularity: DateGranularity = DateGranularity.DAY,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    val isDay = granularity == DateGranularity.DAY

    var expanded by remember { mutableStateOf(false) }
    var yearText by remember(date) { mutableStateOf(date.year.toString()) }
    var monthText by remember(date) { mutableStateOf(date.monthValue.toString()) }
    var dayText by remember(date) { mutableStateOf(date.dayOfMonth.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val today = remember { LocalDate.now() }

    fun step(delta: Long) {
        onDateChange(if (isDay) date.plusDays(delta) else date.plusMonths(delta))
        error = null
    }

    fun apply() {
        val y = yearText.toIntOrNull()
        val m = monthText.toIntOrNull()
        val d = if (isDay) dayText.toIntOrNull() ?: 1 else 1
        if (y == null || m == null) {
            error = "请输入有效的年、月"
            return
        }
        // 非法日期（2月30日、13月等）不能让应用崩，给提示并保留原日期
        val parsed = runCatching { LocalDate.of(y, m, d) }.getOrNull()
        if (parsed == null) {
            error = "日期不存在，请检查（如 2 月最多 29 天）"
            return
        }
        error = null
        expanded = false
        onDateChange(parsed)
    }

    GlassCard(level = GlassLevel.Thin) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── 常态：‹ 日期 › ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArrowButton(
                    direction = -1,
                    onClick = { step(-1) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.controlInCard)
                        .clickable { expanded = !expanded }
                        .padding(vertical = MagicSpacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isDay) {
                                "%d年%d月%d日".format(date.year, date.monthValue, date.dayOfMonth)
                            } else {
                                "%d年%d月".format(date.year, date.monthValue)
                            },
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText,
                        )
                        Text(
                            text = if (isDay) "点此输入日期" else "点此输入月份",
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                        )
                    }
                }
                ArrowButton(
                    direction = 1,
                    onClick = { step(1) },
                )
            }

            // ── 展开：输入 + 跳转 + 回到今天 ──
            if (expanded) {
                Spacer(Modifier.height(MagicSpacing.md))
                MagicDivider()
                Spacer(Modifier.height(MagicSpacing.md))

                Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                    NumberField("年", yearText, { yearText = it }, Modifier.weight(1.3f))
                    NumberField("月", monthText, { monthText = it }, Modifier.weight(1f))
                    if (isDay) {
                        NumberField("日", dayText, { dayText = it }, Modifier.weight(1f))
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(MagicSpacing.sm))
                    Text(
                        text = error.orEmpty(),
                        fontSize = MagicType.caption.sp,
                        color = colors.warning,
                    )
                }

                Spacer(Modifier.height(MagicSpacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                    MagicSolidButton(
                        onClick = ::apply,
                        selected = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "跳转",
                            fontSize = MagicType.body.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent,
                        )
                    }
                    // 已在今天时不必显示，避免一个永远点了没反应的按钮
                    if (date != today) {
                        MagicSolidButton(onClick = {
                            error = null
                            expanded = false
                            onDateChange(today)
                        }, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "回到今天",
                                fontSize = MagicType.body.sp,
                                color = colors.secondaryText,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 左右箭头圆钮 —— 折线画法与 MagicBackIcon 同源，方向镜像 */
@Composable
private fun ArrowButton(direction: Int, onClick: () -> Unit) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shapes.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
            val w = size.width
            val h = size.height
            val stroke = h * 0.13f
            val midY = h / 2f
            // direction = -1 指左，+1 指右
            val tipX = if (direction < 0) w * 0.22f else w * 0.78f
            val tailX = if (direction < 0) w * 0.86f else w * 0.14f
            val wing = h * 0.30f
            val wingDir = if (direction < 0) 1f else -1f
            drawLine(
                color = colors.secondaryText,
                start = androidx.compose.ui.geometry.Offset(tipX, midY),
                end = androidx.compose.ui.geometry.Offset(tailX, midY),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = colors.secondaryText,
                start = androidx.compose.ui.geometry.Offset(tipX, midY),
                end = androidx.compose.ui.geometry.Offset(tipX + wing * wingDir, midY - wing),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = colors.secondaryText,
                start = androidx.compose.ui.geometry.Offset(tipX, midY),
                end = androidx.compose.ui.geometry.Offset(tipX + wing * wingDir, midY + wing),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

/** 数字输入：标签在上、框在下，与八字页的 FieldBox 视觉一致 */
@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MagicSpacing.xs))
        MagicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(4)) },
            placeholder = label,
            keyboardType = KeyboardType.Number,
        )
    }
}
