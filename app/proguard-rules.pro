# Keep Xposed entry point
-keep class com.fuckhyperotg.HookEntry { *; }
-keep class com.fuckhyperotg.hooks.** { *; }

# Keep Xposed API classes from being obfuscated or stripped
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Keep ViewBinding and Android components
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(...);
    public static * bind(...);
}

-keep class com.fuckhyperotg.service.OtgTileService { *; }
-keep class com.fuckhyperotg.ui.MainActivity { *; }
