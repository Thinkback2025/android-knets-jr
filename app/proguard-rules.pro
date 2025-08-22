# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep all classes in our package
-keep class com.knets.jr.** { *; }

# Keep Android framework classes
-keep class android.** { *; }
-keep class androidx.** { *; }

# Disable optimizations that cause ResAuto issues
-dontoptimize
-dontobfuscate
-dontpreverify

# Keep resource files and layouts
-keepresources layout/**
-keepresources drawable/**
-keepresources values/**

# Keep attributes that reference resources
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep device admin receiver
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# Keep service classes
-keep class * extends android.app.Service { *; }

# Keep location service classes
-keep class * extends android.location.** { *; }

# Suppress warnings
-dontwarn android.**
-dontwarn androidx.**
-dontwarn com.android.**

# Keep manifest entries
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# XML namespace processing fix
-dontshrink
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

# Keep OkHttp and Gson classes
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.knets.jr.** { *; }