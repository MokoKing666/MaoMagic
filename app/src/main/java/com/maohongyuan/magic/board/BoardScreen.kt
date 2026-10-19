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
import com.maohongyuan.magic.designsystem.glass.LocalMagicTransitionScope
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
    // 转场进行中再点卡片会让两次边界动画互相抢同一个 key，
    // 表现为「卡片弹回原处」或页面叠影。
    //
    // ⚠️ 只在点击回调里读 isTransitionActive，**不要**在组合期读 ——
    // 它是快照状态，组合期读会订阅它，导致板块页在转场开始/结束时被额外重组一次，
    // 实测会把共享转场的激活推迟约 200ms。
    val transition = LocalMagicTransitionScope.current

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
                            // 未实现的系统仍显示卡片（让用户看到全貌），
                            // 但必须在副标题上标明「开发中」—— 否则点下去没反应，
                            // 用户会以为应用坏了。
                            subtitle = if (sys.available) sys.subtitle else "开发中",
                            icon = sys.icon,
                            // 已实现的系统才参与容器变形转场：
                            // 未实现的卡片点下去不跳页，给它加共享边界只会留下
                            // 一个永远配不上对的 key。
                            sharedId = if (sys.available) sys.name else null,
                            onClick = {
                                val busy = transition?.shared?.isTransitionActive == true
                                if (!busy) {
                                    if (sys.available) onOpen(sys) else onUnavailable(sys)
                                }
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
