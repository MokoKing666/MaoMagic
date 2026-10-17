import java.util.Properties

// :app — 猫仙幻术 应用外壳
// AGP 9 起内建 Kotlin 支持，无需再应用 org.jetbrains.kotlin.android
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.maohongyuan.magic"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.maohongyuan.magic"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.1.1"
    }

    /**
     * 发布签名。
     *
     * 密钥库与口令通过 `keystore.properties`（**已 gitignore**）读取，
     * 文件不存在时静默跳过 —— 这样 CI 或他人克隆后仍能构建 debug，
     * 只是产出未签名的 release，不会因缺密钥而整个构建失败。
     */
    signingConfigs {
        create("release") {
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties().apply {
                    propsFile.inputStream().use { load(it) }
                }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            // 有密钥库时用正式签名，否则产出未签名包（不会中断构建）
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // 设置页要显示版本号。AGP 9 默认不生成 BuildConfig，需显式开启。
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/**/LICENSE.txt",
                "kotlin-tooling-metadata.json",
            )
        }
    }
}

/**
 * debug 构建用**时间戳**作为 versionCode。
 *
 * 背景：原先 debug 与 release 共用 `versionCode = 1`。部分厂商 ROM
 * （实测 vivo / Funtouch）会拦截「同版本号覆盖安装」，`adb install -r`
 * 仍会返回 `Success`，但设备上跑的还是旧包 —— 于是改了代码却看不到变化。
 *
 * 用时间戳后每次构建的 versionCode 都严格递增，覆盖安装必定生效。
 * release 仍使用 [android.defaultConfig] 里声明的正式版本号。
 */
androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val stamp = (System.currentTimeMillis() / 1000L).toInt()
        variant.outputs.forEach { output ->
            output.versionCode.set(stamp)
            output.versionName.set("0.1.0-debug.$stamp")
        }
    }
}

// 内建 Kotlin 下 jvmTarget 默认取 android.compileOptions.targetCompatibility，无需显式声明

dependencies {
    // 设计系统
    implementation(project(":designsystem"))

    // 功能模块
    implementation(project(":feature:bazi"))
    implementation(project(":feature:liuyao"))
    implementation(project(":feature:meihua"))
    implementation(project(":feature:almanac"))

    // 核心层
    implementation(project(":core:data"))
    implementation(project(":core:ai"))
    implementation(project(":core:resource"))
    implementation(project(":feature:xiaoliuren"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)

    debugImplementation(libs.compose.ui.tooling)
}
