package com.maohongyuan.magic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.LocalContentBackdrop
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicGlassSurface
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 开源许可页。
 *
 * ⚠️ 本页**不是可选的**。
 *
 * 应用嵌入了霞鹜新致宋 / 霞鹜新晰黑，这两个字体采用 **IPA Font License 1.0**。
 * 该协议第 3 条第 1 款第 2 项（及 FAQ §3.3.2）规定：
 * > 利用者が派生プログラム利用時に、自身の意志でオリジナル状態の IPA フォントに
 * > 戻せない派生プログラムの再配布は禁止されております。
 *
 * 即：**使用者无法自愿将字体恢复为原始 IPA 字体时，禁止再分发。**
 * 因此嵌入 App 必须同时做两件事，缺一即为违规分发：
 * 1. 附上协议全文或 URL
 * 2. 提供恢复到 IPA 原始字体的方法说明
 *
 * 本页两项都做了。改动本页时请保留这两项。
 */
@Composable
fun LicenseScreen(onDismiss: () -> Unit) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(onClick = onDismiss),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MagicSpacing.screenMargin)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        MagicGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = shapes.sheet,
            level = GlassLevel.Thick,
            backdrop = LocalContentBackdrop.current,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MagicSpacing.cardPadding),
            ) {
                Text(
                    text = "开源许可",
                    fontSize = MagicType.title2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                )
                Spacer(Modifier.height(MagicSpacing.md))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MagicSpacing.section),
                ) {
                    // ── 字体 ──
                    Section(
                        title = "字体",
                        body = """
                            霞鹜新致宋 屏幕阅读版（LXGW Neo ZhiSong Screen）
                            霞鹜新晰黑 屏幕阅读版（LXGW Neo XiHei Screen）

                            作者：落霞孤鹜（lxgw）
                            来源：github.com/lxgw/LxgwNeoXiZhi-Screen
                            授权：IPA Font License 1.0

                            本应用对字体做了子集化（仅保留应用所需字符），
                            属于协议允许的「派生程序」。
                        """.trimIndent(),
                    )

                    Section(
                        title = "⚠ 字体的恢复方法（协议强制义务）",
                        body = """
                            根据 IPA Font License 1.0 第 3 条第 1 款第 2 项，
                            使用者无法自愿将字体恢复为原始 IPA 字体时，禁止再分发。
                            因此本应用提供以下恢复方法：

                            To replace this font with the original IPA font,
                            please get the original font from
                            https://moji.or.jp/ipafont/ipafontdownload/
                            and replace the font file in
                            app/src/main/res/font/.

                            协议全文：
                            https://opensource.org/licenses/ipa
                            https://moji.or.jp/ipafont/license
                        """.trimIndent(),
                    )

                    // ── 依赖库 ──
                    Section(
                        title = "第三方库",
                        body = """
                            cn.6tail:lunar
                              历法 / 干支 / 节气 / 黄历 / 八字内核
                              MIT License

                            io.github.kyant0:backdrop
                              流体玻璃渲染引擎
                              Apache License 2.0

                            AndroidX / Jetpack Compose
                              Apache License 2.0

                            chmxn/laozi-perspective
                              「老子视角」解读风格的提示词（改编，无代码依赖）
                              MIT License
                        """.trimIndent(),
                    )

                    Section(
                        title = "本应用",
                        body = "猫仙幻术\nMIT License\n\n不收集任何个人信息，无需账号。",
                    )
                }

                Spacer(Modifier.height(MagicSpacing.md))

                MagicSolidButton(
                    onClick = onDismiss,
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = MagicSpacing.md),
                ) {
                    Text(
                        text = "返回",
                        fontSize = MagicType.body.sp,
                        color = colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    val colors = LocalMagicColors.current
    Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
        Text(
            text = title,
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.secondaryText,
        )
        MagicDivider()
        Text(
            text = body,
            fontSize = MagicType.footnote.sp,
            color = colors.tertiaryText,
            lineHeight = 21.sp,
        )
    }
}
