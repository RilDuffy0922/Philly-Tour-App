package com.owlhacks.phillytour.data

import com.owlhacks.phillytour.BuildConfig

/**
 * API keys and voice settings, injected into [BuildConfig] from the git-ignored root `.env` file
 * (see `.env.example`). Without keys the app still works: it uses the written fun facts and the
 * on-device voice.
 */
object Secrets {
    val geminiApiKey: String? = value(BuildConfig.GEMINI_API_KEY)
    val geminiModel: String = BuildConfig.GEMINI_MODEL.ifBlank { "gemini-3.8-flash" }
    val elevenLabsApiKey: String? = value(BuildConfig.ELEVEN_LABS_API_KEY)
    val elevenLabsVoiceId: String = value(BuildConfig.ELEVEN_LABS_VOICE_ID) ?: "cgSgspJ2msm6clMCkdW9"

    private fun value(raw: String): String? {
        val trimmed = raw.trim()
        return trimmed.takeIf { it.isNotEmpty() && !it.startsWith("YOUR_") }
    }
}
