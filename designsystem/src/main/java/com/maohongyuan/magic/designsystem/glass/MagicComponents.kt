package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 页面大标题区。
 *
 * 与 [MagicNavBar] 的折叠标题配合：未滚动时显示这一块，
 * 滚动超过阈值后由导航栏的 inline title 接管。
 */
@Composable
fun MagicPageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current

    // 大标题参与容器变形转场：从来源卡片的小标题连续放大到这个位置。
    // key 由路由层经 LocalPageTitleSharedKey 下发，页面自己不需要知道配对关系。
    val transition = LocalMagicTransitionScope.current
    val titleKey = LocalPageTitleSharedKey.current
    val titleModifier = if (transition != null && titleKey != null) {
        with(transition) { titleBounds(titleKey) }
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = MagicType.largeTitle.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primaryText,
            modifier = titleModifier,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(MagicSpacing.xs))
            Text(
                text = subtitle,
                fontSize = MagicType.subheadline.sp,
                color = colors.secondaryText,
            )
        }
    }
}

/** 区块标题 */
@Composable
fun MagicSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    Text(
        text = text,
        fontSize = MagicType.headline.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.primaryText,
        modifier = modifier,
    )
}

/** 卡片内小节标签 */
@Composable
fun MagicLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    Text(
        text = text,
        fontSize = MagicType.footnote.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.secondaryText,
        modifier = modifier,
    )
}

/** 说明 / 注释文字 */
@Composable
fun MagicCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    Text(
        text = text,
        fontSize = MagicType.footnote.sp,
        color = colors.tertiaryText,
        modifier = modifier,
    )
}

/** 分割线 */
@Composable
fun MagicDivider(modifier: Modifier = Modifier) {
    val colors = LocalMagicColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.divider)
    )
}

/**
 * 表格单元格。
 *
 * 用于四柱表、六爻卦盘表这类**纵向扫读**的密集数据。
 * 必须配合 `Modifier.weight()` 使用，保证列像素级对齐。
 */
@Composable
fun RowScope.MagicCell(
    text: String,
    weight: Float,
    emphasized: Boolean = false,
    color: Color? = null,
    alignment: TextAlign = TextAlign.Start,
) {
    val colors = LocalMagicColors.current
    Text(
        text = text,
        fontSize = if (emphasized) MagicType.subheadline.sp else MagicType.footnote.sp,
        fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        color = color ?: if (emphasized) colors.primaryText else colors.secondaryText,
        textAlign = alignment,
        modifier = Modifier.weight(weight),
    )
}

/** 表头单元格 */
@Composable
fun RowScope.MagicHeaderCell(
    text: String,
    weight: Float,
    alignment: TextAlign = TextAlign.Start,
) {
    val colors = LocalMagicColors.current
    Text(
        text = text,
        fontSize = MagicType.caption.sp,
        fontWeight = FontWeight.Medium,
        color = colors.tertiaryText,
        textAlign = alignment,
        modifier = Modifier.weight(weight),
    )
}

/** 标签 + 值 一行 */
@Composable
fun MagicInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    val colors = LocalMagicColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MagicSpacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = MagicType.footnote.sp,
            color = colors.tertiaryText,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            fontSize = MagicType.subheadline.sp,
            color = valueColor ?: colors.secondaryText,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 五行旺衰条。
 *
 * @param ratio 0f..1f
 */
@Composable
fun MagicMetricBar(
    label: String,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = MagicType.footnote.sp,
                fontWeight = FontWeight.Medium,
                color = colors.secondaryText,
                modifier = Modifier.width(24.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(colors.divider, shapes.pill),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(color, shapes.pill),
                )
            }
            Spacer(Modifier.width(MagicSpacing.row))
            Text(
                text = "${(ratio * 100).toInt()}%",
                fontSize = MagicType.caption.sp,
                color = colors.tertiaryText,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

/** 小徽标（用于「世」「应」「动」这类标记） */
@Composable
fun MagicBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalMagicShapes.current
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), shapes.pill)
            .padding(horizontal = MagicSpacing.xs, vertical = 2.dp),
    ) {
        Text(
            text = text,
            fontSize = MagicType.caption.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/**
 * 「待实现」标记。
 *
 * UI 阶段用它明确标出哪些位置还没有真实功能 —— 避免误以为已经能用。
 */
@Composable
fun MagicPlaceholderNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Box(
        modifier = modifier
            .background(colors.warning.copy(alpha = 0.12f), shapes.controlInCard)
            .padding(horizontal = MagicSpacing.md, vertical = MagicSpacing.row),
    ) {
        Text(
            text = "⚠ $text",
            fontSize = MagicType.footnote.sp,
            color = colors.warning,
        )
    }
}

/** 空态 */
@Composable
fun MagicEmptyState(
    glyph: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    val colors = LocalMagicColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MagicSpacing.row),
    ) {
        Text(text = glyph, fontSize = MagicType.glyph.sp, color = colors.tertiaryText)
        Text(
            text = title,
            fontSize = MagicType.body.sp,
            fontWeight = FontWeight.Medium,
            color = colors.secondaryText,
        )
        Text(
            text = description,
            fontSize = MagicType.footnote.sp,
            color = colors.tertiaryText,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

/** 可点击的列表行（右侧带箭头） */
@Composable
fun MagicListRow(
    title: String,
    subtitle: String? = null,
    trailing: String? = "›",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    // 可点行也给按压反馈 —— 列表点了没反应会让人以为没点中
    val press = if (onClick != null) rememberGlassPress() else null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (press != null && onClick != null) press.contentModifier(onClick) else Modifier)
            .padding(vertical = MagicSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = MagicType.body.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = MagicType.footnote.sp,
                    color = colors.tertiaryText,
                )
            }
        }
        if (trailing != null) {
            Text(text = trailing, fontSize = MagicType.title3.sp, color = colors.tertiaryText)
        }
    }
}

/** 卡片内的纵向容器，统一间距 */
@Composable
fun MagicCardBody(
    modifier: Modifier = Modifier,
    spacing: Dp = MagicSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}
