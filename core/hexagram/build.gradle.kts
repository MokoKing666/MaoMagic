// :core:hexagram — 64 卦 / 六爻装卦 / 梅花易数算法内核
// 纯 JVM：禁止出现任何 android.* 导入。本模块是全项目技术核心。
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
    api(project(":core:model"))

    // 卦名/卦辞库以 JSON 资源形式随模块发布
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
