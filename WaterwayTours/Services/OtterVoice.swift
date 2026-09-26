import Foundation

/// An ElevenLabs premade voice the rider can pick for the otter.
struct OtterVoice: Identifiable, Hashable {
    let id: String
    let name: String
    let blurb: String

    static let storageKey = "otterVoiceID"

    /// Premade voices, all checked against a free ElevenLabs account. Any other voice ID can be entered by hand.
    static let all: [OtterVoice] = [
        OtterVoice(id: "cgSgspJ2msm6clMCkdW9", name: "Jessica", blurb: "Playful and bright"),
        OtterVoice(id: "FGY2WhTYpPnrIDTdsKH5", name: "Laura", blurb: "Upbeat and quirky"),
        OtterVoice(id: "IKne3meq5aSn9XLyUdCD", name: "Charlie", blurb: "Energetic and cheerful"),
        OtterVoice(id: "TX3LPaxmHKxFdv7VOQHJ", name: "Liam", blurb: "Young and energetic"),
        OtterVoice(id: "XrExE9yKIg1WjnnlVkGX", name: "Matilda", blurb: "Friendly and warm"),
        OtterVoice(id: "pFZP5JQG7iQjIQuC4Bku", name: "Lily", blurb: "Warm storyteller"),
        OtterVoice(id: "EXAVITQu4vr4xnSDxMaL", name: "Sarah", blurb: "Soft and friendly"),
        OtterVoice(id: "bIHbv24MWmeRgasZH58o", name: "Will", blurb: "Relaxed and optimistic"),
        OtterVoice(id: "SAz9YHcvj6GT2YYXdXww", name: "River", blurb: "Calm and easygoing"),
        OtterVoice(id: "JBFqnCBsd6RMkjVDRZzb", name: "George", blurb: "Warm and grandfatherly"),
        OtterVoice(id: "nPczCjzI2devNBz1zQrb", name: "Brian", blurb: "Deep and comforting"),
    ]
}
