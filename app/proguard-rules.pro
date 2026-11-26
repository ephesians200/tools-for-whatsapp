# Vosk uses JNA (reflection + JNI) — R8 breaks it without these.
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
-keep class org.vosk.** { *; }
-dontwarn java.awt.**
# LAME MP3 encoder (JNI)
-keep class com.naman14.androidlame.** { *; }
# Navigation type-safe routes (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.prince.whatstoolbox.**$$serializer { *; }
-keepclassmembers class com.prince.whatstoolbox.** { *** Companion; }
-keepclasseswithmembers class com.prince.whatstoolbox.** { kotlinx.serialization.KSerializer serializer(...); }
