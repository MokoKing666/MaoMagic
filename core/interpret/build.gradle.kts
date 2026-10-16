// :core:interpret — 统一解卦引擎抽象 + Tier1 规则引擎
// 纯 JVM：禁止出现任何 android.* 导入。
// 本模块必须能完全独立运行（离线、零网络、零模型），是最终兜底。
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
    api(project(":core:hexagram"))
    api(project(":core:calendar"))

    // DivinationInterpreter.interpret 返回 Flow，属公开 API
    api(libs.kotlinx.coroutines.core)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
