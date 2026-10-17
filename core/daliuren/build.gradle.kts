// :core:daliuren — 大六壬
//
// 纯 JVM：禁止任何 android.* 导入。
//
// ⚠️ 实验性：本实现采用最通行的「月将加时 → 四课 → 贼克/比用/涉害取三传」，
// 但三传的九法里只完整实现前三种，其余（昴星/别责/八专/伏吟/反吟）
// 走的是简化路径。**尚未与经典课例对拍**，界面上必须标注。
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

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
