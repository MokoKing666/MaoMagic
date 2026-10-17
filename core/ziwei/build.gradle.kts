// :core:ziwei — 紫微斗数
//
// 纯 JVM：禁止任何 android.* 导入。
//
// 表驱动：定五行局 → 起紫微 → 紫微系逆布 / 天府系顺布 → 十二宫 → 四化。
// 本版只做 14 主星 + 十二宫 + 主星四化；辅星/杂曜后续补。
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
    api(project(":core:calendar"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
