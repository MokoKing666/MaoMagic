package com.maohongyuan.magic.board

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.glass.MagicBoardGroupTitle
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSystemCard
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType
import com.maohongyuan.magic.navigation.AppTab
import com.maohongyuan.magic.navigation.SystemId

/** 网格列数。固定 2 —— 3 列会把中文标题挤成两行，见 MagicSystemCard 注释。 */
private const val COLUMNS = 2

/**
 * 板块首页：按分组列出该板块的全部系统。
 *
 * ⚠️ **网格用 `Column + Row` 手写，不能用 `LazyVerticalGrid`。**
 * 逐项 `drawBackdrop` 会让每张卡都复制背景并跑 RenderEffect，滚动严重掉帧
 * （ARCHITECTURE.md 明令禁止）。整页的玻璃由 [MagicPageContent] 外那一层提供。
 */
@Composable
fun BoardScreen(
    board: AppTab,
    scrollState: ScrollState,
    onOpen: (SystemId) -> Unit,
    onUnavailable: (SystemId) -> Unit,
) {
    val colors = LocalMagicColors.current
    val groups = board.groups

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = board.label, subtitle = headerSubtitle(board))

        if (groups == null) {
            SystemGrid(SystemId.of(board), onOpen, onUnavailable)
        } else {
            groups.forEach { group ->
                val items = SystemId.of(board, group)
                if (items.isEmpty()) return@forEach
                MagicBoardGroupTitle(group)
                SystemGrid(items, onOpen, onUnavailable)
                Spacer(Modifier.height(MagicSpacing.md))
            }
        }

        Spacer(Modifier.height(MagicSpacing.md))
        Text(
            text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(top = MagicSpacing.sm),
        )
    }
}

private fun headerSubtitle(board: AppTab): String = when (board) {
    AppTab.TRADITIONAL -> "命理 · 占卜"
    AppTab.MODERN -> "心理 · 象征"
    AppTab.DAILY -> "黄历 · 运势"
    AppTab.SETTINGS -> ""
}

@Composable
private fun SystemGrid(
    items: List<SystemId>,
    onOpen: (SystemId) -> Unit,
    onUnavailable: (SystemId) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MagicSpacing.cardGap),
    ) {
        items.chunked(COLUMNS).forEach { row ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.cardGap),
            ) {
                row.forEach { sys ->
                    Box(modifier = Modifier.weight(1f)) {
                        MagicSystemCard(
                            title = sys.title,
                            subtitle = sys.subtitle,
                            icon = sys.icon,
                            onClick = {
                                if (sys.available) onOpen(sys) else onUnavailable(sys)
                            },
                        )
                    }
                }
                // 末行不足时补空位，保证卡片左对齐而不是被拉伸
                repeat(COLUMNS - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
