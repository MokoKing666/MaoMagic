// :core:tarot — 塔罗（韦特体系）
//
// 纯 JVM：禁止任何 android.* 导入。
//
// 牌义数据依 A. E. Waite《The Pictorial Key to the Tarot》(1911) 译写，
// 该著作已进入公有领域。文字为中文重述，不取自任何现代出版物
// （现代塔罗书受版权保护，其具体文字不可复制）。
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
