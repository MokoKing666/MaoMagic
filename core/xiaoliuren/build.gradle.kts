// :core:xiaoliuren — 小六壬（六宫速断）
//
// 纯 JVM：禁止任何 android.* 导入。
//
// 小六壬是三式之外最简的占法：六宫循环，按「月→日→时」逐级顺数落宫。
// 算法本身只有三行取模，真正的坑在「起数起点」—— 见 XiaoLiuRenDivination 注释。
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

    // DivinationInterpreter / Interpretation 在本模块实现
    api(project(":core:interpret"))

    // 六宫断语走 JSON 资源（本模块是资源基建的第二个使用者）
    implementation(project(":core:resource"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
