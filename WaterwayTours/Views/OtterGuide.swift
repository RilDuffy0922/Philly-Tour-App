import SwiftUI

/// The otter mascot. While a tour is running it walks you to the next stop, sharing a fun fact in a speech bubble
/// (written by Gemini, read aloud by ElevenLabs when keys are configured).
struct OtterGuide: View {
    let stop: Stop
    let isTracking: Bool
    /// True while something else (like a stop sheet with its own narration) has the floor.
    let isPaused: Bool

    @Environment(Narrator.self) private var narrator
    @Environment(OtterDialogue.self) private var dialogue
    @AppStorage("otterVoiceOn") private var voiceOn = true
    @State private var index = 0
    @State private var restart = 0

    private let readingPause: Duration = .seconds(3)
    private let silentInterval: Duration = .seconds(10)

    private var messages: [String] {
        isTracking
            ? ["Follow me to \(stop.name)!"] + dialogue.lines(for: stop)
            : ["Tap Start tour and I'll guide you to \(stop.name)!"]
    }

    private var message: String { messages[index % messages.count] }
    private var speaks: Bool { voiceOn && narrator.hasCloudVoice }

    var body: some View {
        HStack(alignment: .bottom, spacing: 6) {
            Image("Otter")
                .resizable()
                .scaledToFill()
                .frame(width: 60, height: 60)
                .clipShape(Circle())
                .overlay(Circle().stroke(.white, lineWidth: 2))
                .shadow(radius: 2)
                .accessibilityHidden(true)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.primary)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .id(message)
                .transition(.opacity)
                .padding(.leading, 14)
                .padding(.trailing, narrator.hasCloudVoice ? 36 : 14)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(SpeechBubble().fill(.regularMaterial))
                .overlay(SpeechBubble().stroke(.secondary.opacity(0.25), lineWidth: 1))
                .overlay(alignment: .topTrailing) {
                    if narrator.hasCloudVoice {
                        Button {
                            voiceOn.toggle()
                        } label: {
                            Image(systemName: voiceOn ? "speaker.wave.2.fill" : "speaker.slash.fill")
                                .font(.footnote)
                                .padding(10)
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.secondary)
                        .accessibilityLabel(voiceOn ? "Mute otter voice" : "Unmute otter voice")
                    }
                }
                .accessibilityLabel("Otter says: \(message)")
        }
        .demoTarget(.otter)
        .contentShape(Rectangle())
        .onTapGesture { advance(); restart += 1 }
        .onChange(of: stop.id) { index = 0 }
        .task(id: stop.id) { await dialogue.prepare(stop) }
        .task(id: "\(stop.id)|\(isTracking)|\(isPaused)|\(speaks)|\(restart)") { await talk() }
    }

    /// Says the current line, waits for it to finish, pauses, then moves to the next one.
    private func talk() async {
        guard isTracking, !isPaused else { return }
        var lastSpoken: String?
        defer {
            // Cut off our own line if the tour paused or the voice was muted; leave anyone else's narration alone.
            if let lastSpoken, narrator.isSpeaking(lastSpoken) { narrator.stop() }
        }
        while !Task.isCancelled {
            if speaks {
                let line = message
                lastSpoken = line
                narrator.speak(line, systemVoiceFallback: false)
                // Poll until the line has been read (or give up after a while).
                var waited = 0
                while narrator.isSpeaking(line), waited < 160, !Task.isCancelled {
                    try? await Task.sleep(for: .milliseconds(250))
                    waited += 1
                }
                try? await Task.sleep(for: readingPause)
            } else {
                try? await Task.sleep(for: silentInterval)
            }
            if Task.isCancelled { break }
            advance()
        }
    }

    private func advance() {
        guard messages.count > 1 else { return }
        withAnimation(.easeInOut(duration: 0.25)) { index = (index + 1) % messages.count }
    }
}

/// A rounded rectangle with a small tail on the left pointing at the otter.
struct SpeechBubble: Shape {
    func path(in rect: CGRect) -> Path {
        let tail: CGFloat = 8
        let body = CGRect(x: rect.minX + tail, y: rect.minY, width: rect.width - tail, height: rect.height)
        var path = Path(roundedRect: body, cornerRadius: 16)
        let tailBase = min(rect.maxY - 12, rect.maxY - 22)
        path.move(to: CGPoint(x: body.minX + 1, y: tailBase - 8))
        path.addLine(to: CGPoint(x: rect.minX, y: tailBase + 4))
        path.addLine(to: CGPoint(x: body.minX + 1, y: tailBase + 10))
        path.closeSubpath()
        return path
    }
}

#Preview {
    if let stop = TourLibrary.load().first?.stops.first {
        VStack {
            OtterGuide(stop: stop, isTracking: true, isPaused: false)
            OtterGuide(stop: stop, isTracking: false, isPaused: false)
        }
        .padding()
        .environment(DemoController())
        .environment(Narrator())
        .environment(OtterDialogue())
    }
}
