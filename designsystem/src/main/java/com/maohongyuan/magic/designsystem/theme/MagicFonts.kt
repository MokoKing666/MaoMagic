package com.maohongyuan.magic.designsystem.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.maohongyuan.magic.designsystem.R

/**
 * 纯宋体。只在「不论字重都必须用宋体」的场合显式使用。
 */
val MagicSerif = FontFamily(Font(R.font.lxgw_zhisong_screen, FontWeight.Normal))

/**
 * 纯黑体。UI 标签、按钮用。
 */
val MagicSans = FontFamily(Font(R.font.lxgw_xihei_screen, FontWeight.Normal))

/**
 * 应用默认字体族 —— **字重自动换字面**。
 *
 * ```
 * Normal / Medium   → 霞鹜新致宋（宋体）
 * SemiBold / Bold   → 霞鹜新晰黑（黑体）
 * ```
 *
 * 为什么这么设计：两个字体**都只有单字重**。若把层级建立在 `fontWeight` 上，
 * Android 会走**合成加粗** —— 宋体的横画本来就细，合成加粗会把横竖一起撑粗，
 * 结果是糊成一团，直接违反「可读性高于一切」。
 *
 * 换成黑体则得到**真实的字面差异**，而且符合中文书籍「黑体标题 + 宋体正文」的传统配对。
 *
 * ⚠️ 这是一个**隐式替换**：改 `fontWeight` 会连带换字面。
 *    如果某处需要「宋体且加粗」，那是做不到的 —— 要么换字号，要么接受黑体。
 *    要「不论字重都是宋体」，显式传 [MagicSerif]。
 */
val MagicText = FontFamily(
    Font(R.font.lxgw_zhisong_screen, FontWeight.Normal),
    Font(R.font.lxgw_zhisong_screen, FontWeight.Medium),
    Font(R.font.lxgw_xihei_screen, FontWeight.SemiBold),
    Font(R.font.lxgw_xihei_screen, FontWeight.Bold),
)
