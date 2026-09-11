-keepattributes *Annotation*
-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn javax.annotation.**

# Vendor SDK entry points may be reached through reflection or JNI.
-keep class com.datecs.** { *; }
-keep class com.newland.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn com.datecs.**
-dontwarn com.newland.**
-dontwarn net.sqlcipher.**

# Remove diagnostic output from minified demo/release artifacts.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void print(...);
    public void println(...);
}
