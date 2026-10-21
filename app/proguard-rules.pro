# Keep generated protobuf types and their reflection metadata.
-keep class build.buf.gen.freechat.v1.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessage {
    <fields>;
}
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
