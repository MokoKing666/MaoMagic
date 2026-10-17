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
 * 所以同时写一份到应用私有目录 `files/llm.log`，
 * 用 `adb shell run-as <pkg> cat files/llm.log` 就能读到，不依赖 logcat。
 *
 * 文件大小上限 256 KB，超出后截断重写 —— 避免长期运行撑爆存储。
 */
object LlmLog {

    private const val TAG = "MaoMagicLLM"
    private const val MAX_BYTES = 256 * 1024

    @Volatile
    private var file: File? = null

    fun init(context: Context) {
        if (file == null) file = File(context.filesDir, "llm.log")
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
