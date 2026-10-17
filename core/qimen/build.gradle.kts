// :core:qimen — 奇门遁甲（时家 · 转盘）
//
// 纯 JVM：禁止任何 android.* 导入。
//
// 定局表由项目方提供（二十四节气三元定局表），非凭记忆编写 ——
// 72 个局数错一位，整盘九宫全错且不会报错，所以这张表必须有出处。
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
