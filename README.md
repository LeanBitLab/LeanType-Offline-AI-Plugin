# LeanType Offline AI Plugin

A dynamic plugin for the [LeanType Keyboard](https://github.com/LeanBitLab/LeanType) providing 100% on-device neural proofreading, grammar correction, and local LLM text generation powered by **llama.cpp** (`llamacpp-kotlin`).

---

## ✨ Features

- **🧠 100% On-Device AI**: Local inference powered by optimized `llama.cpp` ARM64/x86_64 JNI binaries.
- **🛡️ Zero Internet & Complete Privacy**: Runs entirely offline with zero network connectivity or external telemetry.
- **⚡ Fast Neural Proofreading & Grammar Correction**: Correct spelling, fix punctuation, and refine writing with local GGUF models (e.g. Qwen2.5, Gemma 2, Llama 3, SmolLM).
- **📦 Dynamic Isolated Architecture**: Loaded on-demand via `DexClassLoader` with isolated native libraries and zero footprint when inactive.
- **🔄 Universal Compatibility**: Compatible across **all 4 LeanType flavors** (`Standard`, `Standard Full`, `Offline`, and `Offline Lite`).

---

> [!IMPORTANT]
> - **Do NOT Install as a Standalone App**: Do **not** install this APK directly into Android OS via the package manager. It is loaded dynamically inside LeanType settings.
> - **Model Requirement**: Requires downloading a compatible GGUF quantized model (e.g. `Qwen2.5-0.5B-Instruct-Q4_K_M.gguf` or `Qwen2.5-1.5B-Instruct-Q4_K_M.gguf`) into your device storage.

---

## 🛠️ How it Works

To maintain a lightweight ~9.8 MB core keyboard, the heavy local LLM inference engine and JNI shared libraries (`libllama.so`) are isolated into this dynamic plugin.

At runtime, LeanType loads this plugin dynamically via `OfflineAiLoader` and `PluginClassLoader` when offline proofreading or AI text operations are requested.

---

## 📥 Installation & Setup

### Option 1: In-App Downloader (Online Flavors)
1. In LeanType, open **Settings → Plugins**.
2. Tap **Offline AI** and tap **Download Plugin** to automatically fetch and activate the latest release.
3. Select your local `.gguf` model file from device storage.

### Option 2: Manual Loading (Offline & Offline Lite Flavors)
1. Download `ai_plugin-arm64-v8a.apk` (or `ai_plugin-x86_64.apk`) from the [Latest Releases](https://github.com/LeanBitLab/LeanType-Offline-AI-Plugin/releases/latest).
2. In LeanType, navigate to **Settings → Plugins**.
3. Tap **Load Offline AI plugin** and select the downloaded `.apk` file.
4. Select your downloaded `.gguf` model file in **Settings → Proofread & AI Settings**.

---

## 🏗️ Building From Source

To compile the release APKs from source:

```bash
./gradlew assembleRelease
```

The compiled APKs will be generated at:
- `app/build/outputs/apk/release/ai_plugin-arm64-v8a.apk`
- `app/build/outputs/apk/release/ai_plugin-x86_64.apk`

---

## 📄 License

Licensed under the [Apache License 2.0](LICENSE).
