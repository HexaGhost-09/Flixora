# Suppress missing java.beans classes required by Mozilla Rhino (not available on Android)
-dontwarn java.beans.**

# Keep Rhino JS engine classes needed at runtime
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
