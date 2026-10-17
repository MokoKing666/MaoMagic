package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.Capsule
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType
import kotlin.math.round

/**
 * 流体玻璃滑块。
 *
 * 照文档 `tutorials/glass-slider` 的做法：**滑块的折射源必须同时包含
 * 「页面背景」与「轨道」**，否则滑块划过轨道时不会折射轨道本身，
 * 看起来就像浮在上面的一块普通玻璃。
 *
 * ⚠️ **不要用 layerBackdrop 录制轨道。**
 *    本组件总在 MagicBackdrop 的 layerBackdrop 子树内，再嵌一层会渲染死循环 → SIGSEGV。
 *    滑块只折射环境 backdrop（页面背景），不折射轨道本身。
 *
 * @param steps 分段数。0 表示连续；> 0 时吸附到等分点
 */
@Composable
fun MagicGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    /** 轨道填充色。默认用强调色 */
    trackColor: Color? = null,
    thumbWidth: Dp = 52.dp,
    thumbHeight: Dp = 30.dp,
    trackHeight: Dp = 6.dp,
) {
    val colors = LocalMagicColors.current
    val resolvedBackdrop = backdrop ?: LocalContentBackdrop.current ?: LocalBackdrop.current
    val density = LocalDensity.current

    val span = valueRange.endInclusive - valueRange.start
    require(span > 0f) { "valueRange 的跨度必须大于 0" }

    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun snap(raw: Float): Float {
        val clamped = raw.coerceIn(0f, 1f)
        if (steps <= 0) return clamped
        val segments = steps + 1
        return (round(clamped * segments) / segments).coerceIn(0f, 1f)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbWidthPx = with(density) { thumbWidth.toPx() }
        val totalPx = with(density) { maxWidth.toPx() }
        // 滑块可移动的行程 = 轨道宽度 − 滑块自身宽度
        val travelPx = (totalPx - thumbWidthPx).coerceAtLeast(1f)
        val touchHeight = thumbHeight.coerceAtLeast(44.dp)

        // 轨道：**只画，不录制**。
        //
        // ⚠️ 原实现用 layerBackdrop(trackBackdrop) 把轨道录进独立 backdrop，
        // 让滑块同时折射「页面背景 + 轨道」。但本组件所在的页面子树
        // 已经被 MagicBackdrop 用 layerBackdrop(contentBackdrop) 标记过，
        // 再嵌一层 layerBackdrop 就是**嵌套录制 → 渲染死循环 → SIGSEGV**。
        // 实测设置页（唯一用滑块的页面）点开即闪退，logcat 为
        // `Fatal signal 11 (SIGSEGV), code 2 (SEGV_ACCERR) in RenderThread`。
        //
        // 代价：滑块不再折射轨道本身，只折射页面背景。
        // 这是拿一点视觉效果换稳定性 —— 崩掉的界面谈不上好看。
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(color = trackColor ?: colors.accent, shape = Capsule()),
        )

        // 滑块：同时折射页面背景与轨道
        Box(
            Modifier
                .offset(x = with(density) { (fraction * travelPx).toDp() })
                // ⚠️ 这里原本是 drawBackdrop + lens(chromaticAberration = true)。
                // 实测在设置页会 SIGSEGV（RenderThread, SEGV_ACCERR）——
                // 滑块与它所在的 GlassCard / MagicBackdrop 形成多层折射，渲染线程崩。
                // 改成纯色胶囊：牺牲玻璃质感，换稳定。滑块的交互与读数都不受影响。
                .background(
                    color = if (colors.isLight) {
                        Color.White.copy(alpha = 0.92f)
                    } else {
                        Color.White.copy(alpha = 0.22f)
                    },
                    shape = Capsule(),
                )
                .border(
                    width = 1.dp,
                    color = colors.divider,
                    shape = Capsule(),
                )
                .size(thumbWidth, thumbHeight),
        )

        // 交互层：点哪儿跳哪儿 + 横向拖动。
        // 高度取 max(滑块高, 44dp) 以满足最小可点区域。
        if (enabled) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(touchHeight)
                    .pointerInput(span, steps, travelPx) {
                        detectTapGestures { offset ->
                            val raw = (offset.x - thumbWidthPx / 2f) / travelPx
                            onValueChange(valueRange.start + snap(raw) * span)
                        }
                    }
                    .pointerInput(span, steps, travelPx) {
                        detectHorizontalDragGestures { change, _ ->
                            val raw = (change.position.x - thumbWidthPx / 2f) / travelPx
                            onValueChange(valueRange.start + snap(raw) * span)
                        }
                    },
            )
        }
    }
}

/** 数值 + 滑块的组合行，供设置页使用。 */
@Composable
fun MagicGlassSliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    caption: String? = null,
) {
    val colors = LocalMagicColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = MagicType.body.sp,
                color = colors.primaryText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                fontSize = MagicType.footnote.sp,
                color = colors.secondaryText,
            )
        }
        Spacer(Modifier.height(MagicSpacing.md))
        MagicGlassSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
        if (caption != null) {
            Spacer(Modifier.height(MagicSpacing.xs))
            Text(text = caption, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        }
    }
}
