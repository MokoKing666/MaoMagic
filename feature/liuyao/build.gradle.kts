// :feature:liuyao — 六爻起卦解卦（铜钱/时间/数字/手动 四方式全上）
// AGP 9 起内建 Kotlin 支持，无需再应用 org.jetbrains.kotlin.android
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.maohongyuan.magic.feature.liuyao"
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
    implementation(project(":designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:calendar"))
    implementation(project(":core:hexagram"))
    implementation(project(":core:interpret"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    debugImplementation(libs.compose.ui.tooling)
}
