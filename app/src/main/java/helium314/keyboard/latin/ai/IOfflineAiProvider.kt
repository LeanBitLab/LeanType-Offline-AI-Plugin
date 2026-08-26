// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import android.content.Context

interface IOfflineAiProvider {
    fun getInterfaceVersion(): Int = 1
    fun init(context: Context)
    fun isAvailable(): Boolean
    fun isModelLoaded(): Boolean
    fun loadModel(context: Context, modelPath: String, threads: Int = 4, nCtx: Int = 2048): Boolean
    fun unloadModel()
    fun generate(prompt: String, params: Map<String, Any>? = null): String
    fun proofread(text: String, instruction: String? = null): String
    fun translate(text: String, sourceLang: String, targetLang: String): String
    fun cleanup()
}
