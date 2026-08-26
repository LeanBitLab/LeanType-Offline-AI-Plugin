# Keep the plugin entry point and interfaces
-keep class helium314.keyboard.ai.plugin.OfflineAiProviderImpl {
    public <init>();
    public *;
}
-keep class helium314.keyboard.latin.ai.IOfflineAiProvider { *; }

# Keep llama.cpp JNI and reflection classes
-keep class org.nehuatl.llamacpp.** { *; }
-keepclassmembers class org.nehuatl.llamacpp.** { *; }
-dontwarn org.nehuatl.llamacpp.**
