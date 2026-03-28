# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in F:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class com.omarea.common.ui.**{*;}
-keep class com.omarea.common.shell.**{*;}
-keep class com.omarea.common.shared.**{*;}
-keep class com.omarea.common.model.**{*;}
-keep class com.omarea.overscroll.**{*;}

-keep class com.omarea.krscript.**{*;}
-keep class com.omarea.krscript.ui.**{*;}
-keep class com.omarea.krscript.model.**{*;}
-keep class com.omarea.krscript.config.**{*;}
-keep class com.omarea.krscript.shortcut.**{*;}
-keep class com.omarea.krscript.executor.**{*;}
-keep class com.omarea.krscript.downloader.**{*;}

-keep class com.tool.tree.utils.** { *; }
-keep class com.tool.tree.ui.** { *; }
-keep class com.tool.tree.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }
