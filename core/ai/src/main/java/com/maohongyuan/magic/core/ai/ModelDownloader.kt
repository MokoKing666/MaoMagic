package com.maohongyuan.magic.core.ai

import kotlinx.coroutines.Dispatchers
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
 * 模型下载器。
 *
 * ── 不需要服务器 ──
 * 模型由魔搭托管（阿里 CDN），公开仓库无需鉴权，直链实测支持断点续传。
 *
 * ── 一个必须遵守的细节 ──
 * **不能缓存 CDN 直链。** 魔搭的 API 地址会 302 到带 `auth_key` 签名的 CDN 链接，
 * 那个签名会过期。所以每次都从 [LocalModel.downloadUrl] 请求，让 OkHttp 自己跟随重定向。
 *
 * ── 为什么先写 .part ──
 * 下到一半被杀进程（用户切后台、系统回收），半截文件如果直接叫正式文件名，
 * 下次会当成「已下载」去加载 —— 结果是模型损坏、推理崩。
 * 所以固定写 `.part`，完整后再改名。
 */
class ModelDownloader(
    private val client: OkHttpClient = defaultClient(),
) {

    /**
     * 下载模型到 [dir]。
     *
     * 已存在且大小正确的文件直接返回 [DownloadState.Done]，不重复下。
     * 存在 `.part` 则从断点续传。
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

        val part = File(dir, "${model.fileName}.part")
        var downloaded = if (part.exists()) part.length() else 0L

        // 已下完但没改名（上次在改名之前被杀）
        if (downloaded == model.sizeBytes && model.sizeBytes > 0) {
            if (part.renameTo(target)) {
                emit(DownloadState.Done(target))
                return@flow
            }
        }

        val request = Request.Builder()
            .url(model.downloadUrl)
            .apply {
                if (downloaded > 0) header("Range", "bytes=$downloaded-")
            }
            .build()

        val response = runCatching { client.newCall(request).execute() }
            .getOrElse {
                emit(DownloadState.Failed("连接失败：${it.message}"))
                return@flow
            }

        response.use { resp ->
            // 206 = 续传成功；200 = 服务器忽略了 Range，要从头来
            if (resp.code == 200 && downloaded > 0) {
                downloaded = 0L
                part.delete()
            }
            if (resp.code !in listOf(200, 206)) {
                emit(DownloadState.Failed("服务器返回 ${resp.code}"))
                return@flow
            }

            val body = resp.body ?: run {
                emit(DownloadState.Failed("响应为空"))
                return@flow
            }
            val total = model.sizeBytes.takeIf { it > 0 }
                ?: (downloaded + body.contentLength())

            RandomAccessFile(part, "rw").use { out ->
                out.seek(downloaded)
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        downloaded += n
                        emit(DownloadState.Progress(downloaded, total))
                    }
                }
            }
        }

        // 大小校验：sha256 未登记时，这是唯一的完整性检查
        if (model.sizeBytes > 0 && part.length() != model.sizeBytes) {
            emit(DownloadState.Failed("大小不符：期望 ${model.sizeBytes}，实际 ${part.length()}"))
            return@flow
        }

        if (!part.renameTo(target)) {
            emit(DownloadState.Failed("重命名失败：${part.absolutePath}"))
            return@flow
        }
        emit(DownloadState.Done(target))
    }.flowOn(Dispatchers.IO)

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
