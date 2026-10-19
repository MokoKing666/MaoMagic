// :designsystem — Apple HIG + iOS 流体玻璃（Liquid Glass）设计系统
// 全局玻璃化的唯一 backdrop 由本模块提供，经 CompositionLocal 下发。
//
// 三条铁律（违反会崩溃或效果失效）：
//   1. 必须调用 Modifier.layerBackdrop(backdrop)，否则效果不生效
//   2. 绝不允许在 drawBackdrop 之后再加 layerBackdrop —— 渲染死循环 → SIGSEGV
//      嵌套玻璃必须改用 exportedBackdrop 参数
//   3. 底色要先 drawRect 进 backdrop，否则玻璃边缘出现透明像素
// AGP 9 起内建 Kotlin 支持，无需再应用 org.jetbrains.kotlin.android
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.maohongyuan.magic.designsystem"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// 内建 Kotlin 下 jvmTarget 默认取 android.compileOptions.targetCompatibility，无需显式声明

dependencies {
    // 设计系统的公开 API 暴露了 Compose 与 Backdrop 的类型
    // （Shape / Backdrop / Composable），因此必须用 api 而非 implementation，
    // 否则消费方编译时无法解析这些类型。
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.ui.tooling.preview)
    // 共享元素转场的公开 API 里出现了 SharedTransitionScope / AnimatedVisibilityScope，
    // 因此同样用 api 暴露给 feature 与 app 模块。
    api(libs.compose.animation)

    // iOS 流体玻璃效果引擎（Apache-2.0）
    api(libs.backdrop)

    // 连续曲率形状库（Apache-2.0）。公开 API 里出现了 Capsule / RoundedRectangle，故用 api。
    api(libs.kyant.shapes)

    debugImplementation(libs.compose.ui.tooling)
}
