package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule

/**
 * 按下的缩放倍率，由 [LiquidBottomTabs] 下发。
 * 官方标定：1f → 1.2f 随 `pressProgress` 插值。
 */
internal val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

/**
 * 底栏的单个 Tab 项。
 *
 * 照搬自 AndroidLiquidGlass（Apache-2.0，作者 Kyant0）：
 * https://github.com/Kyant0/AndroidLiquidGlass
 *
 * ⚠️ 图标与文字请给**中性色**（`primaryText`）。
 *    选中态的蓝色不是在这里上的 —— 它来自 [LiquidBottomTabs] 里那层
 *    `alpha(0f)` + `ColorFilter.tint(accentColor)` 的着色层，
 *    由气泡折射出来。这里再给 accent 会叠成两种不同的蓝。
 *
 * @param onLongClick 长按回调。本项目用于打开设置
 *                    （顶部导航栏删除后，页面末尾的设置条目是主入口，长按是快捷方式）。
 */
@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(Capsule())
            .combinedClickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val scale = scale()
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
