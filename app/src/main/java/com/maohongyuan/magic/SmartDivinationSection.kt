package com.maohongyuan.magic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.ai.DownloadState
import com.maohongyuan.magic.core.ai.DeviceProfile
import com.maohongyuan.magic.core.ai.LocalModel
import com.maohongyuan.magic.core.ai.ModelDownloader
import com.maohongyuan.magic.designsystem.glass.MagicLabel
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import kotlinx.coroutines.launch

/**
 * 「智能解卦」设置区。
 *
 * 模型不进 APK（1.9 GB），用户按需下载。
 * 下载源是魔搭（阿里 CDN），公开仓库无需鉴权。
 *
 * **规则引擎永远兜底** —— 不下载模型也能正常解卦，
 * 只是结论是规则直接生成的，不如模型写得通顺。
 */
@Composable
internal fun SmartDivinationSection() {
    val context = LocalContext.current
    val colors = LocalMagicColors.current
    val scope = rememberCoroutineScope()

    val device = remember { DeviceProfile.current() }
    val model = remember { LocalModel.candidatesFor(device).first() }
    // 分片数可调：不同网络下最优值不同。
    // 4 是默认；CDN 单连接限速时加到 8 有用，弱网下减到 2 更稳。
    var segments by remember { mutableStateOf(4) }
    val downloader = remember(segments) { ModelDownloader(segments = segments) }
    val dir = remember { ModelDownloader.modelDir(context.filesDir) }
    // 删除后要让 installed 失效 —— 用这个计数触发重组。
    // 必须声明在 installed 之前，否则作用域不对。
    var generation by remember { mutableStateOf(0) }
    val installed = remember(generation, segments) { downloader.installed(dir, model) }

    var state by remember { mutableStateOf<String>("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var busy by remember { mutableStateOf(false) }

    MagicCaption(
        "用端上模型把算法算好的结论写成通顺的话。" +
            "卦理仍由本地规则引擎计算，模型只负责表达 —— 它不会改动用神与旺衰。"
    )
    Spacer(Modifier.height(MagicSpacing.row))

    // 设备与推荐 —— 排查问题时让用户截图这两行就够
    Text(
        text = "机型：${device.summary}",
        fontSize = MagicType.footnote.sp,
        color = colors.secondaryText,
    )
    Text(
        text = "推荐：${model.label}（${model.sizeBytes / 1048576} MB）· ${model.note}",
        fontSize = MagicType.footnote.sp,
        color = colors.tertiaryText,
    )
    Spacer(Modifier.height(MagicSpacing.row))

    // 下载中不让改，避免旧协程拿着旧分片数继续跑
    if (installed == null && !busy) {
        MagicSegmentedControl(
            options = listOf("2 线程", "4 线程", "8 线程"),
            selectedIndex = when (segments) {
                2 -> 0
                8 -> 2
                else -> 1
            },
            onSelect = {
                segments = when (it) {
                    0 -> 2
                    2 -> 8
                    else -> 4
                }
            },
        )
        Spacer(Modifier.height(MagicSpacing.xs))
        MagicCaption("线程越多不一定越快 —— 取决于 CDN 是否按连接限速。慢了就换一档试试。")
        Spacer(Modifier.height(MagicSpacing.row))
    }

    when {
        installed != null -> Column {
            Text(
                text = "已下载，可直接使用",
                fontSize = MagicType.footnote.sp,
                color = colors.accent,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(MagicSpacing.xs))
            // 1.9 GB 不是小数目，得让用户能自己清掉
            Text(
                text = "删除模型（释放 ${installed.length() / 1048576} MB）",
                fontSize = MagicType.footnote.sp,
                color = colors.tertiaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (installed.delete()) {
                            state = "已删除"
                            generation++
                        } else {
                            state = "删除失败"
                        }
                    }
                    .padding(vertical = 4.dp),
            )
            if (state.isNotBlank()) {
                Text(state, fontSize = MagicType.footnote.sp, color = colors.tertiaryText)
            }
        }

        busy -> Column {
            val p = progress
            if (p != null) {
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(
                    text = "${(p * 100).toInt()}%",
                    fontSize = MagicType.footnote.sp,
                    color = colors.secondaryText,
                )
            } else {
                Text("正在连接…", fontSize = MagicType.footnote.sp, color = colors.secondaryText)
            }
            if (state.isNotBlank()) {
                Text(state, fontSize = MagicType.footnote.sp, color = colors.tertiaryText)
            }
        }

        else -> Column {
            Text(
                text = "下载模型（约 ${model.sizeBytes / 1048576} MB，建议连 WiFi）",
                fontSize = MagicType.footnote.sp,
                color = colors.accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !busy) {
                        busy = true
                        state = ""
                        progress = null
                        scope.launch {
                            downloader.download(model, dir).collect { s ->
                                when (s) {
                                    is DownloadState.Progress -> {
                                        progress = if (s.total > 0) {
                                            s.downloaded.toFloat() / s.total
                                        } else {
                                            null
                                        }
                                        state = "${s.downloaded / 1048576} / ${s.total / 1048576} MB"
                                    }
                                    is DownloadState.Done -> {
                                        busy = false
                                        state = "完成"
                                    }
                                    is DownloadState.Failed -> {
                                        busy = false
                                        state = s.message
                                    }
                                }
                            }
                        }
                    }
                    .padding(vertical = 4.dp),
            )
            if (state.isNotBlank()) {
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(state, fontSize = MagicType.footnote.sp, color = colors.tertiaryText)
            }
        }
    }

    Spacer(Modifier.height(MagicSpacing.xs))
    MagicCaption("不下模型也能用 —— 规则引擎始终可用，只是文字更朴素。")
}
