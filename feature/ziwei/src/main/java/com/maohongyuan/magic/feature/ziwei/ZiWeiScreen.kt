package com.maohongyuan.magic.feature.ziwei

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.ziwei.Palace
import com.maohongyuan.magic.core.ziwei.PalaceName
import com.maohongyuan.magic.core.ziwei.ZiWeiChart
import com.maohongyuan.magic.core.ziwei.ZiWeiService
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.glass.MagicTextField
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 紫微斗数。
 *
 * 本版做**十四主星 + 十二宫 + 主星四化**。
 * 辅星、杂曜、大限流年未做 —— 这是已知范围，不是遗漏。
 */
@Composable
fun ZiWeiScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current

    var y by remember { mutableStateOf("1990") }
    var m by remember { mutableStateOf("6") }
    var d by remember { mutableStateOf("15") }
    var h by remember { mutableStateOf("12") }
    var gender by remember { mutableStateOf("MALE") }
    var chart by remember { mutableStateOf<ZiWeiChart?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun cast() {
        val yy = y.toIntOrNull()
        val mm = m.toIntOrNull()
        val dd = d.toIntOrNull()
        val hh = h.toIntOrNull()
        if (yy == null || mm == null || dd == null || hh == null ||
            yy !in 1900..2100 || mm !in 1..12 || dd !in 1..31 || hh !in 0..23
        ) {
            error = "请检查：年 1900–2100，月 1–12，日 1–31，时 0–23"
            chart = null
            return
        }
        error = null
        chart = runCatching {
            ZiWeiService.chart(
                yy, mm, dd, hh,
                gender = if (gender == "FEMALE") Gender.FEMALE else Gender.MALE,
            )
        }.getOrElse {
            error = it.message ?: "排盘失败"
            null
        }
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "紫微斗数", subtitle = "十四主星 · 十二宫")

        GlassCard {
            MagicSectionTitle("生辰")
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicCaption("请填【公历（阳历）】出生时间。")

            Spacer(Modifier.height(MagicSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                NumField("公历年", y, { y = it }, Modifier.weight(1.4f))
                NumField("月", m, { m = it }, Modifier.weight(1f))
                NumField("日", d, { d = it }, Modifier.weight(1f))
                NumField("时", h, { h = it }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(MagicSpacing.md))
            MagicSegmentedControl(
                options = listOf("男", "女"),
                selectedIndex = if (gender == "FEMALE") 1 else 0,
                onSelect = { gender = if (it == 1) "FEMALE" else "MALE" },
            )

            if (error != null) {
                Spacer(Modifier.height(MagicSpacing.md))
                Text("⚠ $error", fontSize = MagicType.footnote.sp, color = colors.warning)
            }

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicSolidButton(onClick = ::cast, selected = true, modifier = Modifier.fillMaxWidth()) {
                Text("排盘", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
        }

        chart?.let { c ->
            // ── 盘要 ──
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicSectionTitle("盘要", modifier = Modifier.weight(1f))
                    MagicBadge(c.wuXingJu.label, colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.md))
                Row(modifier = Modifier.fillMaxWidth()) {
                    KeyValue("命宫", c.mingPalaceBranch.label, Modifier.weight(1f))
                    KeyValue("身宫", c.shenPalaceBranch.label, Modifier.weight(1f))
                    KeyValue("紫微", c.ziWeiBranch.label, Modifier.weight(1f))
                }
                Spacer(Modifier.height(MagicSpacing.sm))
                MagicCaption(
                    "${c.solarDate} · 农历${c.lunarMonth}月${c.lunarDay}日 ${c.hourBranch.label}时 · 生年干 ${c.yearGan.label}"
                )
            }

            // ── 十二宫 ──
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("十二宫")
                Spacer(Modifier.height(MagicSpacing.xs))
                MagicCaption("按地支子丑寅…顺序排列。星后括注为该星的四化。")
                Spacer(Modifier.height(MagicSpacing.md))

                c.palaces.forEachIndexed { i, p ->
                    if (i > 0) {
                        Spacer(Modifier.height(MagicSpacing.sm))
                        MagicDivider()
                        Spacer(Modifier.height(MagicSpacing.sm))
                    }
                    PalaceRow(p, isMing = p.name == PalaceName.MING)
                }
            }

            MagicCaption(
                "本版只安十四主星与主星四化；辅星、杂曜、大限流年未做。" +
                    "戊己辛壬四干各有一化落在辅星（右弼/文曲/文昌/左辅），故这四干盘上只见三化。"
            )
        }

        Text(
            text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(vertical = MagicSpacing.row),
        )
    }
}

@Composable
private fun KeyValue(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalMagicColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = MagicType.title3.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
    }
}

@Composable
private fun PalaceRow(p: Palace, isMing: Boolean) {
    val colors = LocalMagicColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = p.name.label,
                fontSize = MagicType.footnote.sp,
                fontWeight = if (isMing) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isMing) colors.accent else colors.secondaryText,
                modifier = Modifier.weight(1f),
            )
            Text(p.branch.label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        }
        if (p.stars.isNotEmpty()) {
            Spacer(Modifier.height(MagicSpacing.xs))
            Text(
                text = p.stars.joinToString("　") { s ->
                    if (s.hua != null) "${s.name}(${s.hua})" else s.name
                },
                fontSize = MagicType.footnote.sp,
                color = colors.primaryText,
            )
        }
    }
}

@Composable
private fun NumField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(8.dp))
        MagicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(4)) },
            placeholder = label,
        )
    }
}
