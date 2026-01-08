# 保留 Retrofit 注解信息 + 泛型签名
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# 保留所有 Retrofit 的 Service 接口（带 retrofit2.http 注解的方法）
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 如果你的 Service 在固定包里，建议更精准（把包名改成你的）
# -keep interface com.yourpkg.**ApiService { *; }
# -keep interface com.yourpkg.api.** { *; }

# Hilt / DI
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Retrofit 及数据模型（涵盖 remote 包，确保泛型不被折叠）
-keep class com.massager.app.data.remote.** { *; }

# Gson TypeToken / TypeAdapter（保留泛型信息）
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class ** extends com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.TypeAdapter { *; }

# Room（本地数据库 schema）
-keep class androidx.room.** { *; }
-keep class com.massager.app.data.local.** { *; }

# Kotlinx serialization metadata
-keepclassmembers class com.massager.app.data.remote.dto.** {
    *** Companion;
}
-keepclassmembers class kotlinx.serialization.** { *; }
