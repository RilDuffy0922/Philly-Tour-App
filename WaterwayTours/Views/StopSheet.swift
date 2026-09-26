import SwiftUI

struct StopSheet: View {
    let stop: Stop
    let session: TourSession
    let onArrive: () -> Void

    @Environment(Narrator.self) private var narrator
    @Environment(\.dismiss) private var dismiss

    private var isVisited: Bool { session.visited.contains(stop.id) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text(stop.narrationScript)
                        .font(.body)

                    let isPlaying = narrator.isSpeaking(stop.narrationScript)
                    Button {
                        isPlaying ? narrator.stop() : narrator.speak(stop.narrationScript)
                    } label: {
                        Label(isPlaying ? "Stop narration" : "Play narration",
                              systemImage: isPlaying ? "stop.fill" : "speaker.wave.2.fill")
                    }
                    .buttonStyle(.bordered)

                    Divider()

                    if isVisited {
                        TriviaCard(trivia: stop.trivia, selected: session.answers[stop.id]) { index in
                            session.answer(index, for: stop)
                        }
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Narration starts automatically when you're within \(Int(stop.radius)) m of this stop. Trivia unlocks when you arrive.")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                            Button("I'm here", systemImage: "mappin.and.ellipse", action: onArrive)
                                .buttonStyle(.borderedProminent)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("\(session.number(of: stop)). \(stop.name)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                Button("Done") { dismiss() }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

private struct TriviaCard: View {
    let trivia: TriviaQuestion
    let selected: Int?
    let onSelect: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Trivia", systemImage: "questionmark.bubble.fill")
                .font(.headline)
                .foregroundStyle(.tint)
            Text(trivia.question)
                .font(.body.weight(.medium))

            ForEach(Array(trivia.options.enumerated()), id: \.offset) { index, option in
                Button {
                    onSelect(index)
                } label: {
                    HStack {
                        Text(option)
                            .multilineTextAlignment(.leading)
                        Spacer()
                        if let symbol = symbol(for: index) {
                            Image(systemName: symbol)
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(background(for: index), in: RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .disabled(selected != nil)
            }

            if let selected {
                Text(selected == trivia.correctIndex ? "Correct!" : "Not quite — it's \(trivia.options[trivia.correctIndex]).")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(selected == trivia.correctIndex ? .green : .orange)
            }
        }
    }

    private func symbol(for index: Int) -> String? {
        guard let selected else { return nil }
        if index == trivia.correctIndex { return "checkmark.circle.fill" }
        if index == selected { return "xmark.circle.fill" }
        return nil
    }

    private func background(for index: Int) -> Color {
        guard let selected else { return Color(.secondarySystemBackground) }
        if index == trivia.correctIndex { return .green.opacity(0.2) }
        if index == selected { return .red.opacity(0.2) }
        return Color(.secondarySystemBackground)
    }
}
