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

# Add project specific ProGuard rules here.
# proguard compile warning 무시하도록 설정
-dontwarn javax.naming.directory.*
-dontwarn javax.naming.*

-dontwarn com.samsung.android.**
-dontwarn org.apache.log4j.**
-dontwarn com.google.android.gms.**
-dontwarn com.vp.fido.**
-dontwarn etri.fido.uaf.**
-dontwarn javax.microedition.khronos.**
-dontwarn org.slf4j.LoggerFactory
-dontwarn org.slf4j.impl.StaticLoggerBinder

-dontwarn com.lumensoft.**
-dontwarn com.raonsecure.kswireless2.**

-dontwarn org.apache.commons.collections.BeanMap
-dontwarn java.beans.**

# SQLite DB에서 문제생기는 것 보정
-keep class etri.fido.auth.sw.db.AuthDBHelper { *; }
-keepclassmembers class etri.fido.auth.sw.db.AuthDBHelper { *; }
-keep class etri.fido.sw.asm.db.ASMDBHelper { *; }
-keepclassmembers class etri.fido.sw.asm.db.ASMDBHelper { *; }

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

-keep class org.spongycastle.jcajce.** { *; }
-keep class org.spongycastle.jce.** { *; }
-keep class etri.fido.** { *; }

-keep class com.samsung.android.sdk.** { *; }

-keep class com.vp.fido.Constants { *; }
-keep class com.vp.fido.VPCManager { *; }
-keep class com.vp.fido.VPCManager$VPCManagerCallback { *; }
-keep class com.vp.fido.util.ErrorMsg { *; }
-keep class com.vp.fido.verify.VerifyResult { *; }

-keep class android.os.SystemProperties { *; }
-keep class com.vp.fido.util.Utilities { *; }

-keep class com.lumensoft.** { *; }
-keep class com.raonsecure.kswireless2.** { *; }

-keep class org.slf4j.** { *; }

##---------------End: proguard configuration for Gson  ----------

# ****************************************************************************
#							FIDO Authenticators ]]
# ****************************************************************************
