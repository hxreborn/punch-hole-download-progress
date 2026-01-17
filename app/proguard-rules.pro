# Keep Xposed module entry point
-keep class eu.hxreborn.orbit.OrbitModule { *; }

# Keep hooker classes and their companion objects
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
-keepclassmembers class * {
    @io.github.libxposed.api.annotations.BeforeInvocation *;
    @io.github.libxposed.api.annotations.AfterInvocation *;
}

# Keep PrefsManager for remote preferences
-keep class eu.hxreborn.orbit.prefs.PrefsManager { *; }

# Keep Xposed detection method from being optimized away
-keep class eu.hxreborn.orbit.ui.MainActivity {
    public static boolean isXposedEnabled();
}
