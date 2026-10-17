package com.maohongyuan.magic.feature.qimen

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.qimen.QiMenChart
import com.maohongyuan.magic.core.qimen.QiMenPalace
import com.maohongyuan.magic.core.qimen.QiMenService
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.glass.MagicTextField
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/** 九宫在界面上的排布（洛书）：4 9 2 / 3 5 7 / 8 1 6 */
private val GRID = listOf(
    listOf(4, 9, 2),
    listOf(3, 5, 7),
    listOf(8, 1, 6),
)

/**
 * 奇门遁甲（时家 · 转盘）。
 *
 * 定局表由项目方提供，非凭记忆编写。
 */
@Composable
fun QiMenScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current

    var y by remember { mutableStateOf("2026") }
    var m by remember { mutableStateOf("10") }
    var d by remember { mutableStateOf("17") }
    var h by remember { mutableStateOf("12") }
    var chart by remember { mutableStateOf<QiMenChart?>(null) }
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
        chart = runCatching { QiMenService.chart(yy, mm, dd, hh) }.getOrElse {
            error = it.message ?: "排盘失败"
            null
        }
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "奇门遁甲", subtitle = "时家 · 转盘")

        GlassCard {
            MagicSectionTitle("起局")
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicCaption("按起局时的时间起局，不是生辰。")

            Spacer(Modifier.height(MagicSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                NumField("年", y, { y = it }, Modifier.weight(1.4f))
                NumField("月", m, { m = it }, Modifier.weight(1f))
                NumField("日", d, { d = it }, Modifier.weight(1f))
                NumField("时", h, { h = it }, Modifier.weight(1f))
            }

            if (error != null) {
                Spacer(Modifier.height(MagicSpacing.md))
                Text("⚠ $error", fontSize = MagicType.footnote.sp, color = colors.warning)
            }

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicSolidButton(onClick = ::cast, selected = true, modifier = Modifier.fillMaxWidth()) {
                Text("起局", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
        }

        chart?.let { c ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicSectionTitle("局要", modifier = Modifier.weight(1f))
                    MagicBadge("${c.dun.label}${c.ju}局", colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.md))
                MagicInfoRow("日期", c.solarDate)
                MagicInfoRow("时柱", c.hourGanZhi)
                MagicInfoRow("节气", "${c.jieQi} · ${c.yuan.label}")
                MagicInfoRow("旬首", c.xunShou)
            }

            // ── 九宫 ──
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("九宫")
                Spacer(Modifier.height(MagicSpacing.xs))
                MagicCaption("按洛书排布：上排 4·9·2，中排 3·5·7，下排 8·1·6。")
                Spacer(Modifier.height(MagicSpacing.md))

                GRID.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MagicSpacing.sm),
                    ) {
                        row.forEach { gong ->
                            c.palaceOf(gong)?.let { p ->
                                PalaceCell(p, Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(MagicSpacing.sm))
                }
            }

            MagicCaption(
                "时家奇门转盘。定局表采用项目方提供的二十四节气三元定局表；" +
                    "八门、八神的布法各家略有出入，本版取通行的一种。"
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
private fun PalaceCell(p: QiMenPalace, modifier: Modifier = Modifier) {
    val colors = LocalMagicColors.current
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(p.shen, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Text(
            text = p.xing,
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.Medium,
            color = colors.accent,
        )
        Text(
            text = p.men ?: "—",
            fontSize = MagicType.footnote.sp,
            color = if (p.men == null) colors.tertiaryText else colors.primaryText,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = p.diPan,
                fontSize = MagicType.title3.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText,
            )
            if (p.tianPan != p.diPan) {
                Text(
                    text = "/${p.tianPan}",
                    fontSize = MagicType.caption.sp,
                    color = colors.secondaryText,
                )
            }
        }
        Text(
            text = "${p.gong}宫",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
            textAlign = TextAlign.Center,
        )
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
        Spacer(Modifier.height(MagicSpacing.xs))
        MagicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(4)) },
            placeholder = label,
        )
    }
}
