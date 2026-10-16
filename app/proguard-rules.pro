# 猫仙幻术 ProGuard/R8 规则
# 发布版默认不开启混淆（isMinifyEnabled = false）。
# 开启混淆时需保留：
#   1. kotlinx.serialization 生成的序列化器
#   2. cn.6tail:lunar 通过反射访问的内部类（如有）

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# kotlinx.serialization
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 领域模型（供序列化器反射使用）
-keep class com.maohongyuan.magic.core.model.** { *; }
