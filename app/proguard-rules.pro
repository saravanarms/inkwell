# R8 / Proguard rules for Inkwell
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# JNI & NDK bindings
-keepclasseswithmembernames class * {
    native <methods>;
}
