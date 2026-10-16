package com.maohongyuan.magic.designsystem.glass.internal

import androidx.compose.runtime.withFrameNanos

/**
 * 等待下一帧。
 *
 * 官方实现里这个函数来自它自己的 `Coroutines.kt` 包装（KMP 用）。
 * 本项目是 Android-only，直接用 Compose 的 `withFrameNanos` 即可，不必搬那个包装。
 *
 * 用途：`DampedDragAnimation.release()` 里需要先让 value 的动画推进一帧，
 * 才能判断它是否已经到达 targetValue，否则会在动画尚未启动时就读到旧值。
 */
internal suspend fun awaitFrame() {
    withFrameNanos { }
}
