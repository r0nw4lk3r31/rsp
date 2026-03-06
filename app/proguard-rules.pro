# Add project specific ProGuard rules here.
-keep class com.sporen.app.** { *; }

# Apache POI
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**

# Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

