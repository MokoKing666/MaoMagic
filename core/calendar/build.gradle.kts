// :core:calendar — 历法内核（公历/农历/干支/节气/黄历/八字）
// 纯 JVM：禁止出现任何 android.* 导入。lunar 库封装在内部，不对外暴露。
plugins {
    alias(libs.plugins.kotlin.jvm)
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

    // cn.6tail:lunar 1.7.7 (MIT) — 无第三方依赖，支持 Java 1.5+
    implementation(libs.tyme4kt)
    // lunar 只在对拍测试里当 oracle —— **不进运行时依赖**，
    // 所以不会打包进 APK。迁移完成后可以删掉这行。
    testImplementation(libs.lunar)

    testImplementation(libs.junit)
}
