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

# RoyalOnBoardImage customizes these legacy renderer paints via reflection.
-keepclassmembers class games.mrlaki5.backgammon.GameView.OnBoardImage {
    private android.graphics.Paint RedChipPaint;
    private android.graphics.Paint WhiteChipPaint;
    private android.graphics.Paint BorderChipPaint;
    private android.graphics.Paint TextPaint;
}

# Tapsell Plus SDK references optional ad-network adapters that are not bundled.
# Suppress R8 missing-class warnings for all of them.
-dontwarn com.adcolony.**
-dontwarn com.unity3d.ads.**
-dontwarn com.unity3d.services.**
-dontwarn com.vungle.**
-dontwarn com.ironsource.**
-dontwarn com.applovin.**
-dontwarn com.chartboost.**
-dontwarn com.mopub.**
-dontwarn ir.tapsell.plus.adnetworks.**
# AdMob / IMA SDK — referenced by Tapsell but not in the foss/bazaar dependency tree
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.ads.interactivemedia.**
