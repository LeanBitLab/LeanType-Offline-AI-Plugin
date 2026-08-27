# Keep the plugin entry point and interfaces
-keep interface helium314.keyboard.latin.ai.IOfflineAiProvider {
    <methods>;
}
-keep class helium314.keyboard.latin.ai.** { *; }
-keep interface helium314.keyboard.latin.ai.** { *; }

-keep class helium314.keyboard.ai.plugin.OfflineAiProviderImpl {
    public <init>();
    <methods>;
    public *;
}

# Keep llama.cpp JNI and reflection classes
-keep class org.nehuatl.llamacpp.** { *; }
-keepclassmembers class org.nehuatl.llamacpp.** { *; }
-dontwarn org.nehuatl.llamacpp.**
-keep class io.github.ljcamargo.llamacpp.** { *; }
-keepclassmembers class io.github.ljcamargo.llamacpp.** { *; }
-dontwarn io.github.ljcamargo.llamacpp.**
