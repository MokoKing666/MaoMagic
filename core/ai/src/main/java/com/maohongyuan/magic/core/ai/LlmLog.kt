package com.maohongyuan.magic.core.ai

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 端上推理的诊断日志。
 *
 * ── 为什么不只用 Logcat ──
 * 部分国产 ROM（实测 vivo）会限制三方应用的 logcat 输出 ——
 * `Log.i` 调用成功但日志不出现在缓冲区，排查时一片空白，
 * 会误判成「代码没执行」。
 *
 * 所以同时写一份到**外部私有目录** `/sdcard/Android/data/<pkg>/files/llm.log`，
 * 用 `adb shell cat //sdcard/Android/data/<pkg>/files/llm.log` 就能读到。
 *
 * ⚠️ 不要改回内部 `filesDir`：那里只有 `run-as` 能读，而 `run-as` 只对
 * debuggable 包有效 —— release 出问题时就彻底没日志了（踩过）。
 *
 * 文件大小上限 256 KB，超出后截断重写 —— 避免长期运行撑爆存储。
 */
object LlmLog {

    private const val TAG = "MaoMagicLLM"
    private const val MAX_BYTES = 256 * 1024

    @Volatile
    private var file: File? = null

    fun init(context: Context) {
        if (file == null) {
            // ⚠️ 优先写**外部私有目录**（/sdcard/Android/data/<pkg>/files/llm.log）。
            //
            // 内部 filesDir 只有 `run-as` 能读，而 `run-as` 只对 debuggable 包有效 ——
            // release 包出问题时根本取不到日志，只能让用户装 debug 版重下 920 MB 模型。
            // 外部私有目录 adb shell 可以直接 `ls` / `cat`，不需要 root，也不需要 debuggable。
            //
            // 这个目录不需要任何权限（应用自己的外部私有空间），卸载即清理。
            val ext = context.getExternalFilesDir(null)
            file = File(ext ?: context.filesDir, "llm.log")
        }
    }

    fun i(msg: String) = write("I", msg, null)

    fun w(msg: String, t: Throwable? = null) = write("W", msg, t)

    fun e(msg: String, t: Throwable? = null) = write("E", msg, t)

    private fun write(level: String, msg: String, t: Throwable?) {
        val line = buildString {
            append(System.currentTimeMillis())
            append(' ')
            append(level)
            append(' ')
            append(msg)
            if (t != null) {
                append(" | ")
                append(t.javaClass.simpleName)
                append(": ")
                append(t.message)
            }
        }
        when (level) {
            "E" -> Log.e(TAG, line)
            "W" -> Log.w(TAG, line)
            else -> Log.i(TAG, line)
        }
        val f = file ?: return
        runCatching {
            if (f.exists() && f.length() > MAX_BYTES) f.writeText("")
            f.appendText(line + "\n")
        }
    }
}
