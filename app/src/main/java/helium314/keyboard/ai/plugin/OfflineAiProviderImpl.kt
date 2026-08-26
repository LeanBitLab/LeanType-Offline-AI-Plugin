// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.ai.plugin

import android.content.Context
import android.net.Uri
import android.util.Log
import helium314.keyboard.latin.ai.IOfflineAiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.nehuatl.llamacpp.LlamaHelper

class OfflineAiProviderImpl : IOfflineAiProvider {
    companion object {
        private const val TAG = "OfflineAiProviderImpl"
    }

    private var appContext: Context? = null
    private var initialized = false

    private var llamaHelper: LlamaHelper? = null
    private var currentModelPath: String? = null
    private var isLoaded: Boolean = false
    private val loadMutex = Mutex()

    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val llmFlow = MutableSharedFlow<LlamaHelper.LLMEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun getInterfaceVersion(): Int = 1

    override fun init(context: Context) {
        this.appContext = context.applicationContext ?: context
        this.initialized = true
        Log.i(TAG, "OfflineAiProviderImpl initialized")
    }

    override fun isAvailable(): Boolean = initialized

    override fun isModelLoaded(): Boolean = isLoaded && llamaHelper != null

    override fun loadModel(context: Context, modelPath: String, threads: Int, nCtx: Int): Boolean = runBlocking {
        loadMutex.withLock {
            if (isLoaded && currentModelPath == modelPath && llamaHelper != null) {
                return@runBlocking true
            }

            unloadModelInternal()

            try {
                val ctx = appContext ?: context
                val contentResolver = ctx.contentResolver
                val helper = LlamaHelper(
                    contentResolver,
                    scope,
                    llmFlow
                )

                val llamaField = LlamaHelper::class.java.getDeclaredField("llama\$delegate").apply { isAccessible = true }
                val llamaLazy = llamaField.get(helper) as Lazy<org.nehuatl.llamacpp.LlamaAndroid>
                val llama = llamaLazy.value

                val uri = Uri.parse(modelPath)
                val pfd = contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalArgumentException("Failed to open model file descriptor")
                val modelFd = pfd.detachFd()

                val optimalThreads = if (threads > 0) threads else {
                    val cores = Runtime.getRuntime().availableProcessors()
                    if (cores <= 4) cores else 4
                }

                Log.i(TAG, "Loading GGUF model: path=$modelPath threads=$optimalThreads nCtx=$nCtx")

                val params = mutableMapOf<String, Any>(
                    "model" to modelPath,
                    "model_fd" to modelFd,
                    "use_mmap" to false,
                    "use_mlock" to false,
                    "n_ctx" to (if (nCtx > 0) nCtx else 2048),
                    "embedding" to false,
                    "n_batch" to 512,
                    "n_threads" to optimalThreads,
                    "n_gpu_layers" to 0,
                    "vocab_only" to false,
                    "lora" to "",
                    "lora_scaled" to 1.0,
                    "rope_freq_base" to 0.0,
                    "rope_freq_scale" to 0.0
                )

                val callback: (String) -> Unit = { word ->
                    try {
                        val allTextField = LlamaHelper::class.java.getDeclaredField("allText").apply { isAccessible = true }
                        val currentAllText = allTextField.get(helper) as String
                        allTextField.set(helper, currentAllText + word)

                        val tokenCountField = LlamaHelper::class.java.getDeclaredField("tokenCount").apply { isAccessible = true }
                        val currentCount = tokenCountField.get(helper) as Int
                        tokenCountField.set(helper, currentCount + 1)

                        helper.sharedFlow.tryEmit(LlamaHelper.LLMEvent.Ongoing(word, currentCount + 1))
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in native token callback", e)
                    }
                }

                val result = llama.startEngine(params, callback)
                val contextId = result?.get("contextId") as? Int
                    ?: throw IllegalStateException("contextId not found in engine start result")

                val currentContextField = LlamaHelper::class.java.getDeclaredField("currentContext").apply { isAccessible = true }
                currentContextField.set(helper, contextId)

                helper.sharedFlow.tryEmit(LlamaHelper.LLMEvent.Loaded(modelPath))

                llamaHelper = helper
                currentModelPath = modelPath
                isLoaded = true
                Log.i(TAG, "Model loaded successfully into plugin engine")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load GGUF model in plugin", e)
                isLoaded = false
                false
            }
        }
    }

    override fun unloadModel() {
        runBlocking {
            loadMutex.withLock {
                unloadModelInternal()
            }
        }
    }

    private fun unloadModelInternal() {
        try {
            llamaHelper?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error unloading llama model", e)
        }
        llamaHelper = null
        currentModelPath = null
        isLoaded = false
    }

    override fun generate(prompt: String, params: Map<String, Any>?): String {
        val helper = llamaHelper ?: throw IllegalStateException("Model not loaded in AI plugin")

        val generatedText = StringBuilder()
        var errorMessage: String? = null

        val temp = (params?.get("temperature") as? Number)?.toFloat() ?: 0.2f
        val topP = (params?.get("top_p") as? Number)?.toFloat() ?: 0.95f
        val topK = (params?.get("top_k") as? Number)?.toInt() ?: 40
        val minP = (params?.get("min_p") as? Number)?.toFloat() ?: 0.05f
        val maxTokens = (params?.get("max_tokens") as? Number)?.toInt() ?: 256

        runBlocking {
            val eventJob = launch {
                helper.sharedFlow.takeWhile { event ->
                    when (event) {
                        is LlamaHelper.LLMEvent.Ongoing -> {
                            generatedText.append(event.word)
                            true
                        }
                        is LlamaHelper.LLMEvent.Done -> {
                            false
                        }
                        is LlamaHelper.LLMEvent.Error -> {
                            errorMessage = event.toString()
                            false
                        }
                        else -> true
                    }
                }.collect {}
            }

            predictWithParams(
                helper = helper,
                prompt = prompt,
                temp = temp,
                topP = topP,
                topK = topK,
                minP = minP,
                maxTokens = maxTokens
            )

            eventJob.join()
        }

        if (errorMessage != null) {
            throw RuntimeException("Inference error: $errorMessage")
        }
        return generatedText.toString().trim()
    }

    override fun proofread(text: String, instruction: String?): String {
        if (text.isBlank()) return text
        val instr = instruction ?: "Fix grammar, spelling, and punctuation errors. Return ONLY the corrected text without preamble."
        val prompt = "<|im_start|>system\nYou are a professional proofreader. Correct grammatical, spelling, and punctuation errors while preserving original formatting.<|im_end|>\n<|im_start|>user\nInstruction: $instr\nText: $text<|im_end|>\n<|im_start|>assistant\n"
        val out = generate(prompt, mapOf("temperature" to 0.1, "max_tokens" to 512))
        return cleanOutput(out)
    }

    override fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (text.isBlank()) return text
        val prompt = "<|im_start|>system\nYou are a professional translator. Translate from $sourceLang to $targetLang. Output ONLY the translation without preamble.<|im_end|>\n<|im_start|>user\nText: $text<|im_end|>\n<|im_start|>assistant\n"
        val out = generate(prompt, mapOf("temperature" to 0.2, "max_tokens" to 512))
        return cleanOutput(out)
    }

    private fun cleanOutput(text: String): String {
        return text
            .replace(Regex("<thinking>[\\s\\S]*?</thinking>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<thought>[\\s\\S]*?</thought>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<reasoning>[\\s\\S]*?</reasoning>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<details>[\\s\\S]*?</details>", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun predictWithParams(
        helper: LlamaHelper,
        prompt: String,
        temp: Float,
        topP: Float,
        topK: Int,
        minP: Float,
        maxTokens: Int
    ) {
        try {
            val currentContextField = LlamaHelper::class.java.getDeclaredField("currentContext").apply { isAccessible = true }
            val currentContext = currentContextField.get(helper) as? Int ?: throw IllegalStateException("Model context not ready")

            val llamaField = LlamaHelper::class.java.getDeclaredField("llama\$delegate").apply { isAccessible = true }
            val llamaLazy = llamaField.get(helper) as Lazy<org.nehuatl.llamacpp.LlamaAndroid>
            val llama = llamaLazy.value

            val tokenCountField = LlamaHelper::class.java.getDeclaredField("tokenCount").apply { isAccessible = true }
            tokenCountField.set(helper, 0)

            val allTextField = LlamaHelper::class.java.getDeclaredField("allText").apply { isAccessible = true }
            allTextField.set(helper, "")

            helper.sharedFlow.tryEmit(LlamaHelper.LLMEvent.Started(prompt))

            val params = mutableMapOf<String, Any>(
                "prompt" to prompt,
                "emit_partial_completion" to true,
                "temperature" to temp.toDouble(),
                "top_p" to topP.toDouble(),
                "top_k" to topK,
                "min_p" to minP.toDouble(),
                "n_predict" to maxTokens,
                "stop" to listOf("\nInput:", "\nInstruction:", "\nOutput:", "\nCorrected:", "<|im_end|>")
            )

            val completionJobField = LlamaHelper::class.java.getDeclaredField("completionJob").apply { isAccessible = true }

            val job = helper.scope.launch {
                val startTime = System.currentTimeMillis()
                try {
                    llama.launchCompletion(currentContext, params)
                } catch (e: Throwable) {
                    Log.e(TAG, "Completion failed", e)
                    helper.sharedFlow.tryEmit(LlamaHelper.LLMEvent.Error("Completion failed: ${e.message}"))
                    return@launch
                }
                val duration = System.currentTimeMillis() - startTime
                val allText = allTextField.get(helper) as String
                val tokenCount = tokenCountField.get(helper) as Int
                helper.sharedFlow.tryEmit(LlamaHelper.LLMEvent.Done(allText, tokenCount, duration))
            }
            completionJobField.set(helper, job)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to setup prediction", e)
            helper.sharedFlow.tryEmit(LlamaHelper.LLMEvent.Error("Failed to setup prediction: ${e.message}"))
        }
    }

    override fun cleanup() {
        unloadModel()
        appContext = null
        initialized = false
    }
}
