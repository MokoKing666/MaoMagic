package com.maohongyuan.magic.feature.daliuren

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
import com.maohongyuan.magic.core.daliuren.DaLiuRenChart
import com.maohongyuan.magic.core.daliuren.DaLiuRenService
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.glass.MagicTextField
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 大六壬。
 *
 * ⚠️ **实验性**：三传只完整覆盖贼克 / 比用 / 涉害，其余取法走简化路径，
 * 未与经典课例对拍。界面上必须让用户看见这一点。
 */
@Composable
fun DaLiuRenScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current

    var y by remember { mutableStateOf("2026") }
    var m by remember { mutableStateOf("10") }
    var d by remember { mutableStateOf("17") }
    var h by remember { mutableStateOf("12") }
    var chart by remember { mutableStateOf<DaLiuRenChart?>(null) }
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
        chart = runCatching { DaLiuRenService.chart(yy, mm, dd, hh) }.getOrElse {
            error = it.message ?: "起课失败"
            null
        }
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "大六壬", subtitle = "四课三传 · 实验性")

        GlassCard {
            MagicSectionTitle("起课")
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicCaption("按起课时的时间起天地盘，不是生辰。")

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
                Text("起课", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
        }

        chart?.let { c ->
            // ── 课要 ──
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicSectionTitle("课要", modifier = Modifier.weight(1f))
                    MagicBadge(c.yueJiang.label + "将", colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.md))
                MagicInfoRow("日期", c.solarDate)
                MagicInfoRow("日柱", c.dayGanZhi)
                MagicInfoRow("时支", c.hourBranch.label + "时")
                MagicInfoRow("月将", "${c.yueJiang.label}将（${c.zhongQi}后）")
            }

            // ── 四课 ──
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("四课")
                Spacer(Modifier.height(MagicSpacing.md))
                Row(modifier = Modifier.fillMaxWidth()) {
                    c.ke.forEach { k ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(k.label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
                            Spacer(Modifier.height(MagicSpacing.xs))
                            Text(
                                text = k.shang.label,
                                fontSize = MagicType.title3.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(k.xiaLabel, fontSize = MagicType.footnote.sp, color = colors.secondaryText)
                        }
                    }
                }
                Spacer(Modifier.height(MagicSpacing.sm))
                MagicCaption("上行为天盘（上神），下行为地盘（下神）。")
            }

            // ── 三传 ──
            GlassCard(level = GlassLevel.Thin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicSectionTitle("三传", modifier = Modifier.weight(1f))
                    MagicBadge(c.method, colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.md))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("初传", "中传", "末传").forEachIndexed { i, label ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
                            Spacer(Modifier.height(MagicSpacing.xs))
                            Text(
                                text = c.sanChuan[i].label,
                                fontSize = MagicType.largeTitle.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                            )
                        }
                    }
                }
            }

            // ── 实验性提示 ──
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("关于本实现")
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(
                    text = "三传的取法有九种（贼克、比用、涉害、昴星、别责、八专、伏吟、反吟等），" +
                        "本版只完整实现了前三种，其余走简化路径。" +
                        "天地盘与四课是确定的，但**三传尚未与经典课例对拍**，请勿据此下断。\n\n" +
                        "这一条不是免责套话 —— 六壬断事全看三传，三传取错则整个课白起。",
                    fontSize = MagicType.footnote.sp,
                    color = colors.secondaryText,
                )
            }
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
