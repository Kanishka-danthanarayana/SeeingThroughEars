package com.ears.vision

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

class GemmaManager(private val context: Context) {
    private var llmInference: LlmInference? = null

    init {
        val modelPath = prepareModelFile()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(512)
            .setTopK(40)
            .setTemperature(0.7f)
            .build()

        try {
            llmInference = LlmInference.createFromOptions(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun prepareModelFile(): String {
        val file = File(context.filesDir, "gemma.bin")
        if (!file.exists()) {
            context.assets.open("gemma-2b-it-gpu-int4.bin").use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }

    fun describeFrame(prompt: String, callback: (String) -> Unit) {
        val formattedPrompt = "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"
        val response = try {
            llmInference?.generateResponse(formattedPrompt) ?: "AI failed to respond."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
        callback(response)
    }

    fun close() {
        llmInference?.close()
    }
}