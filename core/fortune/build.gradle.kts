// :core:fortune — 日运 / 月运
//
// 纯 JVM：禁止任何 android.* 导入。
//
// 算法：日主 + 流日/流月干支 → 十神 → 映射到事业/财运/感情/健康四维。
// 流日/流月干支直接用 lunar 库算好的结果（CalendarService.ganZhi），不手写推算。
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
    implementation(project(":core:resource"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
