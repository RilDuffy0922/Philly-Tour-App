package com.owlhacks.phillytour.speech

/** An ElevenLabs premade voice the rider can pick for the otter. */
data class OtterVoice(val id: String, val name: String, val blurb: String) {
    companion object {
        const val STORAGE_KEY = "otterVoiceID"

        /** Premade voices, all checked against a free ElevenLabs account. Any other voice ID can be entered by hand. */
        val all: List<OtterVoice> = listOf(
            OtterVoice("cgSgspJ2msm6clMCkdW9", "Jessica", "Playful and bright"),
            OtterVoice("FGY2WhTYpPnrIDTdsKH5", "Laura", "Upbeat and quirky"),
            OtterVoice("IKne3meq5aSn9XLyUdCD", "Charlie", "Energetic and cheerful"),
            OtterVoice("TX3LPaxmHKxFdv7VOQHJ", "Liam", "Young and energetic"),
            OtterVoice("XrExE9yKIg1WjnnlVkGX", "Matilda", "Friendly and warm"),
            OtterVoice("pFZP5JQG7iQjIQuC4Bku", "Lily", "Warm storyteller"),
            OtterVoice("EXAVITQu4vr4xnSDxMaL", "Sarah", "Soft and friendly"),
            OtterVoice("bIHbv24MWmeRgasZH58o", "Will", "Relaxed and optimistic"),
            OtterVoice("SAz9YHcvj6GT2YYXdXww", "River", "Calm and easygoing"),
            OtterVoice("JBFqnCBsd6RMkjVDRZzb", "George", "Warm and grandfatherly"),
            OtterVoice("nPczCjzI2devNBz1zQrb", "Brian", "Deep and comforting"),
        )
    }
}
