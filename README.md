# LeanType Offline AI Plugin

A dynamic plugin for the [LeanType Keyboard](https://github.com/LeanBitLab/LeanType) providing 100% on-device neural proofreading, grammar correction, and local LLM text generation powered by **llama.cpp** (`llamacpp-kotlin`).

---

## ✨ Features

- **🧠 100% On-Device AI**: Local inference powered by optimized `llama.cpp` ARM64/x86_64 JNI binaries.
- **🛡️ Zero Internet & Complete Privacy**: Runs entirely offline with zero network connectivity or external telemetry.
- **⚡ Fast Neural Proofreading & Grammar Correction**: Correct spelling, fix punctuation, and refine writing with local GGUF models (e.g. Qwen2.5, Gemma 2, Llama 3, SmolLM).
- **🔒 Designed for Offline Editions**: Built specifically for **LeanType Offline** and **Offline Lite** flavors with zero internet permissions (Online flavors use built-in Cloud AI).
- **📦 Dynamic Isolated Architecture**: Loaded on-demand via `DexClassLoader` with isolated native libraries and zero footprint when inactive.

---

> [!IMPORTANT]
> - **Do NOT Install as a Standalone App**: Do **not** install this APK directly into Android OS via the package manager. It is loaded dynamically inside LeanType settings.
> - **Model Requirement**: Requires downloading a compatible GGUF quantized model (e.g. `Qwen2.5-0.5B-Instruct-Q4_K_M.gguf` or `Qwen2.5-1.5B-Instruct-Q4_K_M.gguf`) into your device storage.

---

## 📋 System Requirements

- **Operating System**: Android 7.0 (API 24) or higher
- **Supported CPU Architectures**: `arm64-v8a` (64-bit ARM), `x86_64` (64-bit x86)
- **Host Keyboard**: [LeanType](https://github.com/LeanBitLab/LeanType) v4.1.6+ (Offline or Offline Lite flavors)
- **RAM**: Recommended 3 GB+ RAM for 0.5B–1.5B GGUF models

---

## 🛠️ How it Works

To maintain a lightweight ~9.8 MB core keyboard, the heavy local LLM inference engine and JNI shared libraries (`libllama.so`) are isolated into this dynamic plugin.

At runtime, LeanType loads this plugin dynamically via `OfflineAiLoader` and `PluginClassLoader` when offline proofreading or AI text operations are requested.

---

## 📥 Installation & Setup (Offline & Offline Lite Flavors)

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

Licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
