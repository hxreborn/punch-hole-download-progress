# Keep LSPosed module entry point
-keep class eu.hxreborn.phdp.xposed.PHDPModule { *; }

# Xposed module class pattern
-keepattributes RuntimeVisibleAnnotations
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public void onPackageLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}

# Prevent R8 from merging hook classes into app process code (compileOnly API)
# This also covers IndicatorState, which holds the hook-side pref cache.
-keep,allowobfuscation class eu.hxreborn.phdp.xposed.hook.** { *; }

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
