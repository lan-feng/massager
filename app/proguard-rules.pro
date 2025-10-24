# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Retrofit models
-keep class com.massager.app.data.remote.dto.** { *; }

# Room schema
-keep class androidx.room.** { *; }
-keep class com.massager.app.data.local.** { *; }

# Kotlinx serialization metadata
-keepclassmembers class com.massager.app.data.remote.dto.** {
    *** Companion;
}
-keepclassmembers class kotlinx.serialization.** { *; }
