# Keep LSPosed module entry point
-keep class eu.hxreborn.phdp.PunchHoleDownloadProgressModule { *; }

# Keep all hooker classes and their methods
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }

# Keep libxposed API annotations
-keep class io.github.libxposed.api.annotations.** { *; }
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations

# Prevent R8 from removing hook methods
-keepclassmembers class * {
    @io.github.libxposed.api.annotations.BeforeInvocation <methods>;
    @io.github.libxposed.api.annotations.AfterInvocation <methods>;
}

# Xposed module class pattern
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
    public void onPackageLoaded(...);
    public void onSystemServerLoaded(...);
}

# Keep PrefsManager for remote preferences
-keep class eu.hxreborn.phdp.prefs.PrefsManager { *; }

# Keep Xposed detection method
-keep class eu.hxreborn.phdp.ui.MainActivity {
    public static boolean isXposedEnabled();
}

# Kotlin intrinsics optimization
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# Strip debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Obfuscation
-repackageclasses
-allowaccessmodification
