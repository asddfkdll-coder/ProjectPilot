# Keep Room
-keep class androidx.room.** { *; }
-keep @androidx.room.* class * { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.projectpilot.app.**$$serializer { *; }
-keepclassmembers class com.projectpilot.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.projectpilot.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# General security: strip logs in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
