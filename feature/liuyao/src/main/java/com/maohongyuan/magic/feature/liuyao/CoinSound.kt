package com.maohongyuan.magic.feature.liuyao

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 铜钱落桌的音效。
 *
 * ── 音源是合成的，不是采样 ──
 * `res/raw/coin_toss.wav` 由脚本合成：三枚铜钱先后落桌，
 * 每枚用 5 个**非谐分音**（1 : 2.76 : 5.40 : 8.93 : 13.3，自由圆板的模态比），
 * 加 3ms 冲击噪声和指数衰减。
 *
 * 为什么不用现成音效：商业 App 里的音频素材是创作作品，
 * 拷进 MIT 仓库会侵权。合成的声音是原创，没有这个问题。
 * 音色不如真实录音，但「叮」的金属感是对的。
 *
 * 用 SoundPool 而不是 MediaPlayer：这是几十毫秒的短音效，
 * 需要低延迟、可并发、不占播放通道。
 */
@Composable
fun rememberCoinSound(): () -> Unit {
    val context = LocalContext.current

    val pool = remember {
        SoundPool.Builder()
            .setMaxStreams(2)   // 连摇时最多两枚重叠，再多会糊
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    val soundId = remember(pool) {
        pool.load(context, R.raw.coin_toss, 1)
    }

    DisposableEffect(pool) {
        onDispose { pool.release() }
    }

    return remember(pool, soundId) {
        {
            // 音量为 1.0，但素材本身峰值压到 0.82，不会爆音
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}
