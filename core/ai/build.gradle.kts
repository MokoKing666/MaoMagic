// :core:ai — Tier2 云端 AI（OpenAI 兼容）+ Tier3 端上本地模型（LiteRT-LM）
// Tier3 设计为可选：不配模型也必须能正常构建与运行。
// AGP 9 起内建 Kotlin 支持，无需再应用 org.jetbrains.kotlin.android
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    testOptions {
        unitTests {
            // android.util.Log 在 JVM 单测里没有实现，不设这个会抛
            // "Method w in android.util.Log not mocked"
            isReturnDefaultValues = true
        }
    }

    namespace = "com.maohongyuan.magic.core.ai"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// 内建 Kotlin 下 jvmTarget 默认取 android.compileOptions.targetCompatibility，无需显式声明

dependencies {
    api(project(":core:interpret"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    // 端上推理（Tier3）。坐标在 **Google Maven**，不在 Maven Central ——
    // 用 repo1 探测会 404，settings.gradle.kts 里的 google() 才有。

    // LiteRT-LM：支持 .litertlm 容器，有 MediaTek/Qualcomm NPU 运行时。
    // 坐标在 **Google Maven**（不在 Maven Central）。
    implementation(libs.litertlm.android)
    // LiteRT GPU 加速器：libLiteRtClGlAccelerator.so 同时支持 OpenCL 与 OpenGL。
    // 本机 OpenCL 不可达（驱动不在白名单），但 GLES 可用 ——
    // 加上它，LiteRT-LM 的 GPU 后端可能走 GL 而不是 CL。
    implementation("com.google.ai.edge.litert:litert-gpu:2.3.0")
    // LiteRT 核心运行时。官方 sample 里显式声明了它，
    // litert-gpu 的加速器要链接它；不显式写会被动依赖，版本不可控。
    implementation("com.google.ai.edge.litert:litert:2.3.0")
}
