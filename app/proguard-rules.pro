# 猫仙幻术 ProGuard/R8 规则
# 发布版开启 R8 压缩 + 资源裁剪。
# 需保留：
#   1. kotlinx.serialization 生成的序列化器
#   2. androidx.work（WorkManager）反射类 —— 被依赖间接引入，
#      R8 裁掉 WorkDatabase 反射构造器会导致启动崩溃
#   3. Room 反射类 —— WorkDatabase 底层用 Room
#   4. cn.6tail:lunar 通过反射访问的内部类（如有）

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# kotlinx.serialization
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 领域模型（供序列化器反射使用）
-keep class com.maohongyuan.magic.core.model.** { *; }

# ── androidx.work（WorkManager）──
# WorkManager 通过反射实例化 WorkDatabase（Room），
# R8 必须保留这些类及其无参构造器。
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.impl.model.** { *; }

# ── Room（WorkDatabase 底层依赖）──
-keep class androidx.room.** { *; }

# ── LiteRT / litert_jni（端上推理，native 反射）──
-keep class com.google.ai.edge.litert.** { *; }
