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

# Missing optional dependencies (bnd, Saxon, OSGi) — not used on Android
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.framework.**
