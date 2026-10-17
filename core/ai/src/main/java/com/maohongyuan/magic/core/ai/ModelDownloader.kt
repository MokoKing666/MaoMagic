package com.maohongyuan.magic.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/** 下载状态 */
sealed interface DownloadState {
    /** 已下载字节 / 总字节（总字节可能为 0，表示服务器没给 Content-Length） */
    data class Progress(val downloaded: Long, val total: Long) : DownloadState

    /** 完成，文件已就位 */
    data class Done(val file: File) : DownloadState

    data class Failed(val message: String) : DownloadState
}

/**
 * 模型下载器 —— **多线程分片**。
 *
 * ── 为什么要分片 ──
 * 单连接跑不满带宽（实测魔搭 CDN 单连接不到 10 MB/s）。
 * 切成 4 段并行，各自发 `Range` 请求，通常能显著提速。
 *
 * ── 为什么用预分配 + 侧车文件，而不是分片各写一个文件 ──
 * 分片文件最后要合并，1.9 GB 合并时**峰值要 2 倍空间**（3.8 GB）。
 * 手机上这个代价太大。
 *
 * 所以：先 `setLength(总大小)` 预分配，各线程按偏移写入；
 * 每个分片的进度单独记在 `.segs` 侧车文件里 —— 中断后能精确续传，
 * 且不需要额外的大空间。
 *
 * ── 一个必须遵守的细节 ──
 * **不能缓存 CDN 直链。** 魔搭的 API 地址会 302 到带 `auth_key` 签名的 CDN 链接，
 * 签名会过期。所以每次都从 [LocalModel.downloadUrl] 请求，让 OkHttp 自己跟随重定向。
 */
class ModelDownloader(
    private val client: OkHttpClient = defaultClient(),
    /** 分片数。4 在多数网络下已能跑满带宽，再多收益递减且更容易触发风控 */
    private val segments: Int = 4,
) {

    /**
     * 下载模型到 [dir]。
     *
     * 已存在且大小正确的文件直接返回 [DownloadState.Done]，不重复下。
     * 存在侧车文件则从各分片断点续传。
     */
    fun download(model: LocalModel, dir: File): Flow<DownloadState> = flow {
        val target = File(dir, model.fileName)
        if (target.exists() && target.length() == model.sizeBytes) {
            emit(DownloadState.Done(target))
            return@flow
        }

        if (!dir.exists() && !dir.mkdirs()) {
            emit(DownloadState.Failed("无法创建目录：${dir.absolutePath}"))
            return@flow
        }

        val total = model.sizeBytes
        if (total <= 0) {
            emit(DownloadState.Failed("模型大小未登记，无法分片下载"))
            return@flow
        }

        val part = File(dir, "${model.fileName}.part")
        val sidecar = File(dir, "${model.fileName}.segs")

        // 预分配 —— 稀疏文件，长度立刻正确
        if (!part.exists() || part.length() != total) {
            RandomAccessFile(part, "rw").use { it.setLength(total) }
            sidecar.delete()   // 长度变了说明是新的下载，旧进度作废
        }

        val segRanges = split(total)
        val done = readSidecar(sidecar, segRanges.size)

        emit(DownloadState.Progress(done.sum(), total))

        val ok = coroutineScope {
            segRanges.mapIndexed { i, range ->
                async(Dispatchers.IO) {
                    if (done[i] >= range.len) return@async true
                    fetchSegment(model, part, range, i, done[i], sidecar, done)
                }
            }.awaitAll()
        }

        if (ok.any { !it }) {
            emit(DownloadState.Failed("部分分片下载失败，已保留进度，可重试"))
            return@flow
        }

        if (part.length() != total) {
            emit(DownloadState.Failed("大小不符：期望 $total，实际 ${part.length()}"))
            return@flow
        }

        sidecar.delete()
        if (!part.renameTo(target)) {
            emit(DownloadState.Failed("重命名失败：${part.absolutePath}"))
            return@flow
        }
        emit(DownloadState.Done(target))
    }.flowOn(Dispatchers.IO)

    /** 下载一个分片。返回是否成功 */
    private fun fetchSegment(
        model: LocalModel,
        part: File,
        range: LongRange,
        index: Int,
        alreadyDone: Long,
        sidecar: File,
        done: LongArray,
    ): Boolean {
        val start = range.first + alreadyDone
        if (start > range.last) return true

        val request = Request.Builder()
            .url(model.downloadUrl)
            .header("Range", "bytes=$start-${range.last}")
            .build()

        val resp = runCatching { client.newCall(request).execute() }.getOrNull() ?: return false
        resp.use { r ->
            // 206 = 分片成功。200 说明服务器忽略了 Range，分片策略失效
            if (r.code != 206) return false
            val body = r.body ?: return false

            RandomAccessFile(part, "rw").use { out ->
                out.seek(start)
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    var pos = start
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        pos += n
                        done[index] = pos - range.first
                        writeSidecar(sidecar, done)
                    }
                }
            }
        }
        return done[index] >= range.len
    }

    /** 分片字节数 */
    private val LongRange.len: Long get() = last - first + 1

    /** 均分，余数给最后一片 */
    private fun split(total: Long): List<LongRange> {
        val per = total / segments
        return (0 until segments).map { i ->
            val from = i * per
            val to = if (i == segments - 1) total - 1 else (i + 1) * per - 1
            from..to
        }
    }

    private fun readSidecar(file: File, n: Int): LongArray {
        val arr = LongArray(n)
        if (!file.exists()) return arr
        runCatching {
            file.readText().trim().split(",").forEachIndexed { i, s ->
                if (i < n) arr[i] = s.trim().toLongOrNull() ?: 0L
            }
        }
        return arr
    }

    private fun writeSidecar(file: File, done: LongArray) {
        // 每 64 KB 写一次，频率可接受；写失败不影响下载本身
        runCatching { file.writeText(done.joinToString(",")) }
    }

    /** 已下载的模型 */
    fun installed(dir: File, model: LocalModel): File? =
        File(dir, model.fileName).takeIf { it.exists() && it.length() == model.sizeBytes }

    companion object {
        /** 模型目录：应用私有存储，不占用户可见空间 */
        fun modelDir(appFilesDir: File): File = File(appFilesDir, "models")

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            // 2 GB 的下载，默认 10 秒超时必然失败
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)   // 不限总时长
            .retryOnConnectionFailure(true)
            .build()
    }
}
