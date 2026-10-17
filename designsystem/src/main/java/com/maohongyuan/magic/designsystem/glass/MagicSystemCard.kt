package com.maohongyuan.magic.designsystem.glass

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.theme.MagicShapes
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 板块页里的系统入口卡片。
 *
 * ⚠️ **不要在这里再嵌一层玻璃。**
 * 整页由 [MagicPageContent] 外面那一层 backdrop 提供折射，
 * 卡片本身只是「内容玻璃」（[GlassCard] 的 L1 语义），
 * 再嵌一层会触发 backdrop 渲染死循环（见 ARCHITECTURE.md 的三条铁律）。
 *
 * 因此卡片网格必须用 `Column + Row` 手写，**不能**用 `LazyVerticalGrid`
 * 逐项 `drawBackdrop` —— 那样每项都要复制背景并跑 RenderEffect，滚动严重掉帧。
 */
@Composable
fun MagicSystemCard(
    title: String,
    subtitle: String,
    icon: MagicTabIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    // 按压张力：玻璃被按下、朝手指方向拉伸、亮起光斑。
    // 公式抄自 Kyant0/AndroidLiquidGlass 的 LiquidButton。
    val press = rememberGlassPress()

    // ⚠️ 手势必须挂在 GlassCard 的 modifier 上，不能挂在内部的 Row 上。
    // 挂在内部时点击不生效 —— 实测过（顶部那个设置齿轮当初能用，
    // 正是因为它把 clickable 挂在了 MagicGlassSurface 的 modifier 上）。
    GlassCard(
        modifier = modifier
            .clip(shapes.card)
            .then(press.contentModifier(onClick)),
        level = GlassLevel.Thin,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 卡片里的图标尺寸够大，统一用填充态；描边在这个尺寸下会显得单薄
                MagicTabIconView(
                    icon = icon,
                    tint = colors.accent,
                    selected = true,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(MagicSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    // 长名字（5 字以上）降一档字号 + 单行截断。
                    // 原来不限制：中文会任意处断行，「十六型人格」被切成
                    // 「十六型人 / 格」，看着像排版坏了。
                    fontSize = if (title.length >= 5) MagicType.footnote.sp else MagicType.body.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                )
                Text(
                    text = subtitle,
                    fontSize = MagicType.footnote.sp,
                    color = colors.tertiaryText,
                )
            }
            Text(
                text = "›",
                fontSize = MagicType.headline.sp,
                color = colors.tertiaryText,
            )
        }
    }
}

/** 板块页里的分组小标题（如「命理」「占卜」） */
@Composable
fun MagicBoardGroupTitle(text: String) {
    val colors = LocalMagicColors.current
    Text(
        text = text,
        fontSize = MagicType.footnote.sp,
        fontWeight = FontWeight.Medium,
        color = colors.tertiaryText,
        modifier = Modifier.padding(
            start = MagicSpacing.xs,
            top = MagicSpacing.sm,
            bottom = MagicSpacing.xs,
        ),
    )
}

/**
 * 2 列卡片网格。
 *
 * 列数固定 2：360dp 宽下每卡约 158dp，够放「图标 + 标题 + 副标题」；
 * 3 列只剩约 104dp，中文标题会被挤成两行、必须降字号 —— 违反字号阶梯约定。
 */
@Composable
fun MagicSystemGrid(
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MagicSpacing.cardGap),
    ) {
        content(2)
    }
}

/** 网格里的一行 —— 末行不足时用空 Box 补齐，保证左对齐不居中 */
@Composable
fun MagicSystemGridRow(
    columns: Int,
    modifier: Modifier = Modifier,
    cell: @Composable (Int) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MagicSpacing.cardGap),
    ) {
        repeat(columns) { i ->
            Box(modifier = Modifier.weight(1f)) { cell(i) }
        }
    }
}

/** 占位：某格没有卡片时保持高度，避免该行被压扁 */
@Composable
fun MagicSystemGridSpacer() {
    Box(modifier = Modifier.fillMaxSize().height(1.dp))
}
