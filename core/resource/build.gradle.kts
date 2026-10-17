// :core:resource — JSON 资源加载基建
//
// 纯 JVM：禁止任何 android.* 导入。这样单测能直接跑，不依赖模拟器。
//
// 存在的理由：项目原先所有文本都是 Kotlin 硬编码表（如 Hexagram.NAMES）。
// 塔罗 78 张牌义硬编码会有 2000+ 行 Kotlin，不适合。
// 注意 docs/ARCHITECTURE.md 曾宣称「卦名卦辞放 JSON 资源」，但从未落地 ——
// 本模块是第一次真正建立这套基建。
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // JsonResource 的 inline 函数把 kotlinx.serialization 暴露给了调用方
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
