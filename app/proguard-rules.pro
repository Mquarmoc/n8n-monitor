# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep,allowobfuscation,allowshrinking class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep,allowobfuscation,allowshrinking class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# n8n API DTOs
-keep class com.example.n8nmonitor.data.dto.** { *; }

# Keep ViewModels
-keep class com.example.n8nmonitor.ui.viewmodel.** { *; }

# ===== ADVANCED OBFUSCATION =====
# Repackage all classes into a single package for maximum obfuscation
-repackageclasses 'o'
# Allow modification of access modifiers for better optimization
-allowaccessmodification
# Use aggressive method overloading for obfuscation
-overloadaggressively
# Flatten package hierarchy
-flattenpackagehierarchy
# Use unique class member names
-useuniqueclassmembernames

# ===== PERFORMANCE OPTIMIZATIONS =====
# Enable specific optimizations while avoiding problematic ones
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable
# Increase optimization passes for better results
-optimizationpasses 7
# Enable more aggressive optimizations
-allowaccessmodification
-mergeinterfacesaggressively

# ===== SECURITY ENHANCEMENTS =====
# Remove debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Remove debug-related classes and methods
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
}

# ===== STACK TRACE PROTECTION =====
# Keep source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
# Rename source file to generic name
-renamesourcefileattribute SourceFile

# ===== STRING OBFUSCATION =====
# Obfuscate string constants (requires manual implementation)
# Note: Consider using string encryption for sensitive data

# ===== REFLECTION PROTECTION =====
# Keep annotations for reflection-based frameworks
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations

# ===== ANTI-TAMPERING =====
# Keep class names for integrity checks (if implemented)
# -keep class com.n8nmonitor.security.IntegrityChecker { *; }

# ===== GOOGLE ERROR PRONE ANNOTATIONS =====
# Handle missing Google Error Prone annotations
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** { *; }

# Google Tink Crypto Library
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**