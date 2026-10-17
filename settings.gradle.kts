pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MaoXianHuanShu"

// ── 应用外壳 ────────────────────────────────────────────────
include(":app")

// ── 核心层 ─────────────────────────────────────────────────
// 纯 JVM 模块（java-library）：禁止出现任何 android.* 导入
include(":core:model")
include(":core:resource")
include(":core:xiaoliuren")
include(":core:tarot")
include(":core:calendar")
include(":core:hexagram")
include(":core:interpret")
// Android 模块
include(":core:ai")
include(":core:data")

// ── 设计系统 ────────────────────────────────────────────────
include(":designsystem")

// ── 功能模块 ────────────────────────────────────────────────
include(":feature:bazi")
include(":feature:liuyao")
include(":feature:meihua")
include(":feature:almanac")
include(":feature:xiaoliuren")
include(":feature:tarot")
