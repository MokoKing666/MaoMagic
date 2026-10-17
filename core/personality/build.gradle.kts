// :core:personality — 十六型人格（荣格类型）
//
// 纯 JVM：禁止任何 android.* 导入。
//
// ⚠️ 合规：不叫 MBTI（The Myers-Briggs Company 注册商标），
// 不使用 NERIS Type Explorer（16Personalities 专有）的题目或类型别称。
// 题目与类型描述均为自编。
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
    api(project(":core:interpret"))
    implementation(project(":core:resource"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
