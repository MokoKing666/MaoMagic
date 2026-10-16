package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicColors

/**
 * 环境折射源 —— 用于内联玻璃（卡片、列表、按钮）。
 *
 * 不录制内容图层，因此可以安全地用在任意深度，包括 `LazyColumn` 的 item 内部。
 *
 * 实现为**屏幕锚定**：绘制时把屏幕坐标平移到当前玻璃表面的局部坐标系，
 * 于是同一屏内所有卡片折射到的是**同一张底图的同一区域**。
 * 这是修复「卡片颜色随滚动位置漂移」的关键 —— 早期版本用 `rememberCanvasBackdrop`
 * 在局部坐标里画渐变，导致每张卡片拿到的是不同的切片。
 */
val LocalBackdrop = staticCompositionLocalOf<Backdrop> {
    error("MagicBackdrop 未提供。请把内容包在 MagicBackdrop { } 内。")
}

/**
 * 内容折射源 —— 用于覆盖层玻璃（底部 Tab 栏、底部弹窗）。
 *
 * 会把「内容层」整棵子树录制进图层，因此玻璃表面能**真实折射页面内容**。
 *
 * ⚠️ 结构性约束：被 `layerBackdrop` 标记的子树内部**绝不能**出现引用同一个
 *    backdrop 的 `drawBackdrop` —— 录制与读取互相引用会形成渲染死循环，
 *    最终 `Fatal signal 11 (SIGSEGV) in RenderThread` 崩溃。
 */
val LocalContentBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 屏幕锚定的环境折射源。
 *
 * `Backdrop.drawBackdrop` 的绘制发生在**玻璃表面自身的局部坐标系**里。
 * 若直接在局部坐标画渐变，每张卡片都会得到自己尺寸对应的一份渐变，
 * 结果是同一屏内不同卡片的底色不一致（ΔE 很大）。
 *
 * 修正方式：拿到根容器的 `LayoutCoordinates`，算出玻璃表面相对根的位置，
 * 反向平移后按**根容器尺寸**绘制 —— 等价于给每张卡片"裁"下屏幕底图的对应一块。
 */
private class ScreenAnchoredBackdrop(
    private val rootCoordinates: () -> LayoutCoordinates?,
    private val baseColor: Color,
    private val drawScreen: DrawScope.(Size) -> Unit,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        // 无条件先铺一层不透明底色。
        //
        // 这一步是安全网，不是冗余：坐标在首帧或宿主不可用时可能拿不到，
        // 若此时直接 return，玻璃就会退化成「透出下层内容」的半透明块 ——
        // 卡片与弹窗会彻底失去边界（实测：设置弹窗曾整片透出底层页面内容）。
        drawRect(baseColor)

        val root = rootCoordinates() ?: return
        val target = coordinates ?: return
        if (!root.isAttached || !target.isAttached) return

        val screenSize = Size(root.size.width.toFloat(), root.size.height.toFloat())
        if (screenSize.width <= 0f || screenSize.height <= 0f) return

        val offset = try {
            root.localPositionOf(target)
        } catch (_: Exception) {
            return
        }

        withTransform({ translate(-offset.x, -offset.y) }) {
            drawScreen(screenSize)
        }
    }
}

/**
 * 流体玻璃的根容器 —— 全 App 只建**一个**根 backdrop。
 *
 * 三层结构：
 * ```
 * Layer 3  覆盖层   overlay —— 与内容层平级，可真实折射内容
 * Layer 2  内容层   content —— 被录制进 contentBackdrop（内部禁止引用 contentBackdrop 做玻璃）
 * Layer 1  背景层   环境渐变 + 色斑 + 卦纹 + 颗粒 —— 玻璃「看得见」的前提
 * ```
 *
 * 为什么背景必须有内容可折射：纯色背景上做玻璃，与一块实色卡片视觉上毫无区别。
 * 但背景必须是**低饱和**的 —— 高饱和背景会让玻璃卡片透出随机色相，
 * 破坏色彩一致性，也直接损害文字对比度。
 *
 * @param content 页面内容。内部用 [MagicGlassSurface]（走环境折射源）是安全的。
 * @param overlay 覆盖层（底部 Tab 栏、弹窗）。与 content 平级，可用内容折射源。
 */
@Composable
fun MagicBackdrop(
    modifier: Modifier = Modifier,
    glassMode: GlassMode = GlassMode.FULL,
    content: @Composable () -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val colors = LocalMagicColors.current

    // 根容器坐标 —— 屏幕锚定绘制的基准
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val rootProvider = remember { { rootCoordinates } }

    // 环境折射源：屏幕锚定，同一屏内所有卡片透出同一片底色
    val ambientBackdrop = remember(colors, rootProvider) {
        ScreenAnchoredBackdrop(rootProvider, colors.backdropBase) { screenSize ->
            drawMagicAmbient(colors, screenSize)
        }
    }

    // 内容折射源：录制「环境 + 内容层」→ 覆盖层可真实折射页面内容
    val contentBackdrop = rememberLayerBackdrop {
        drawMagicAmbient(colors, size)
        drawContent()
    }

    CompositionLocalProvider(
        LocalBackdrop provides ambientBackdrop,
        LocalContentBackdrop provides contentBackdrop,
        LocalGlassMode provides glassMode,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { if (it.isAttached) rootCoordinates = it },
        ) {
            // ── Layer 1 背景层 ──
            // 必须真实画出来，否则页面本身没有底色，玻璃也失去参照
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawMagicAmbient(colors, size) }
            )

            // ── Layer 2 内容层 ──
            // 被 layerBackdrop 标记；内部只能使用环境折射源做玻璃
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop)
            ) {
                content()
            }

            // ── Layer 3 覆盖层 ──
            // 与内容层平级，可以安全引用 contentBackdrop 做真实折射
            overlay()
        }
    }
}

/**
 * 环境背景绘制。
 *
 * 组成（缺一不可）：
 * 1. 低饱和基调 + 线性渐变 —— 提供整体色域
 * 2. 三个大半径低饱和色斑 —— 提供色相变化与纵深
 * 3. 卦纹底纹（乾卦三阳爻）—— 提供可辨认的语义细节
 * 4. 极淡爻线阵列 —— **给 blur 提供高频细节**。纯渐变无可折射内容，
 *    玻璃会退化成半透明白块；细节层是折射成立的必要条件。
 * 5. 细颗粒噪点 —— 同上，让 blur 有东西可处理
 *
 * @param screenSize 屏幕尺寸。必须传入屏幕尺寸而非局部尺寸，否则会出现色相漂移。
 */
internal fun DrawScope.drawMagicAmbient(colors: MagicColors, screenSize: Size) {
    val w = screenSize.width
    val h = screenSize.height
    if (w <= 0f || h <= 0f) return

    // 1. 基调 + 线性渐变
    drawRect(color = colors.backdropBase)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(colors.backdropBase, colors.backdropGradientEnd),
            start = Offset.Zero,
            end = Offset(w, h),
        )
    )

    // 2. 三个大半径低饱和色斑
    val blobRadius = maxOf(w, h) * 0.62f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colors.blob1, Color.Transparent),
            center = Offset(w * 0.18f, h * 0.12f),
            radius = blobRadius,
        ),
        radius = blobRadius,
        center = Offset(w * 0.18f, h * 0.12f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colors.blob2, Color.Transparent),
            center = Offset(w * 0.88f, h * 0.34f),
            radius = blobRadius * 0.88f,
        ),
        radius = blobRadius * 0.88f,
        center = Offset(w * 0.88f, h * 0.34f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colors.blob3, Color.Transparent),
            center = Offset(w * 0.30f, h * 0.86f),
            radius = blobRadius * 0.94f,
        ),
        radius = blobRadius * 0.94f,
        center = Offset(w * 0.30f, h * 0.86f),
    )

    // 3. 卦纹底纹：乾卦三阳爻 ☰，右下角，极低透明，轻微旋转
    run {
        val barLength = w * 0.20f
        val barGap = barLength * 0.30f
        val barWidth = barLength * 0.05f
        val cx = w * 0.80f
        val cy = h * 0.74f
        rotate(degrees = -18f, pivot = Offset(cx, cy)) {
            for (i in -1..1) {
                val y = cy + i * (barGap + barWidth)
                drawLine(
                    color = colors.watermark,
                    start = Offset(cx - barLength / 2f, y),
                    end = Offset(cx + barLength / 2f, y),
                    strokeWidth = barWidth,
                )
            }
        }
    }

    // 4. 爻线阵列 —— 给 blur 提供高频细节，让折射真的能"折"出东西
    //    6 行，每行 77 段，按行递增密度（左疏右密），极低对比度
    run {
        val cols = 77
        val rows = 6
        val cellW = w / cols
        val cellH = h / (rows + 2)
        val top = h * 0.06f
        val dashH = maxOf(1f, cellH * 0.16f)
        val segW = cellW * 0.62f
        for (r in 0 until rows) {
            val density = (r + 1).toFloat() / rows
            val y = top + r * cellH
            // 左端疏、右端密
            val activeCols = (cols * (0.35f + 0.65f * density)).toInt()
            for (c in 0 until activeCols) {
                drawRect(
                    color = colors.grain,
                    topLeft = Offset(c * cellW, y),
                    size = Size(segW, dashH),
                )
            }
        }
    }

    // 5. 细颗粒噪点（确定性伪随机，避免每帧抖动）
    run {
        val count = 900
        var seed = 0x9E3779B9.toInt()
        repeat(count) {
            seed = seed * 1103515245 + 12345
            val x = ((seed ushr 16) and 0x7FFF) / 32767f * w
            seed = seed * 1103515245 + 12345
            val y = ((seed ushr 16) and 0x7FFF) / 32767f * h
            drawCircle(color = colors.grain, radius = 0.8f, center = Offset(x, y))
        }
    }
}
