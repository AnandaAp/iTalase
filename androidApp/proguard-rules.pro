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

# 1. Coroutines Swing dispatcher references missing on Android
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn kotlinx.coroutines.swing.**

# 2. Prevent R8 from obfuscating and splitting Kotlin stdlib internals (fixes IllegalAccessError with Uuid & text)
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# 3. SQLCipher Android (JNI and native database driver)
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# 4. Room KMP (generated DB implementations and constructors)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabaseConstructor { *; }
-keep class **_Impl { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# 5. Koin DI
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# 6. Compose Multiplatform Generated Resources
-keep class com.anlian.italase.shared.generated.resources.** { *; }

# 7. Third-party utilities
-dontwarn io.github.aakira.napier.**
-dontwarn kotlinx.datetime.**
