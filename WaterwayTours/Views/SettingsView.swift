import SwiftUI

/// Otter voice options and a way to replay the tutorial.
struct SettingsView: View {
    let onShowTutorial: () -> Void

    @Environment(Narrator.self) private var narrator
    @Environment(\.dismiss) private var dismiss
    @AppStorage("otterVoiceOn") private var voiceOn = true
    @AppStorage(OtterVoice.storageKey) private var voiceID = ""
    @State private var customID = ""

    private var selectedID: String { voiceID.isEmpty ? narrator.voiceID : voiceID }

    var body: some View {
        NavigationStack {
            Form {
                if narrator.hasCloudVoice {
                    Section {
                        Toggle("Otter speaks aloud", isOn: $voiceOn)
                    }

                    Section {
                        ForEach(OtterVoice.all) { voice in
                            Button {
                                choose(voice.id)
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(voice.name).foregroundStyle(.primary)
                                        Text(voice.blurb).font(.caption).foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    if selectedID == voice.id {
                                        Image(systemName: "checkmark").foregroundStyle(.tint)
                                    }
                                }
                            }
                        }
                    } header: {
                        Text("Otter voice")
                    } footer: {
                        Text("Tap a voice to hear it and pick it.")
                    }

                    Section {
                        TextField("ElevenLabs voice ID", text: $customID)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                        Button("Use this voice") { choose(customID.trimmingCharacters(in: .whitespacesAndNewlines)) }
                            .disabled(customID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    } header: {
                        Text("Custom voice")
                    } footer: {
                        Text("Paste any voice ID from your ElevenLabs account.")
                    }
                } else {
                    Section {
                        Text("Add your ElevenLabs key to Secrets.plist to give the otter a voice.")
                            .foregroundStyle(.secondary)
                    }
                }

                Section {
                    Button("Replay the tutorial", systemImage: "questionmark.circle") {
                        dismiss()
                        onShowTutorial()
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                Button("Done") { dismiss() }
            }
        }
    }

    private func choose(_ id: String) {
        guard !id.isEmpty else { return }
        voiceID = id
        narrator.speak("Hi! I'm your otter guide. Ready to explore?", systemVoiceFallback: false, voiceID: id)
    }
}
