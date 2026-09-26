import SwiftUI

/// A short first-run walkthrough. Shown once automatically, and any time from the "?" button.
struct TutorialView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var page = 0

    private struct Page {
        let title: String
        let text: String
        let symbol: String?
    }

    private let pages: [Page] = [
        Page(title: "Meet your otter guide",
             text: "I'll take you along Philadelphia's rivers and share fun facts about the places we pass.",
             symbol: nil),
        Page(title: "Pick a tour",
             text: "Choose the Schuylkill or the Delaware. When you start, the stop closest to you becomes stop 1.",
             symbol: "map.fill"),
        Page(title: "Tap Start tour and follow me",
             text: "I'll chat about the next stop as you go. When you arrive, you'll hear its story and get a trivia question.",
             symbol: "location.fill"),
        Page(title: "Make it yours",
             text: "Not interested in a stop? Tap Skip. Want to try it from your couch? Open the ••• menu to set your location or run a demo. Change my voice in Settings.",
             symbol: "slider.horizontal.3"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $page) {
                ForEach(pages.indices, id: \.self) { index in
                    VStack(spacing: 24) {
                        Spacer()
                        if let symbol = pages[index].symbol {
                            Image(systemName: symbol)
                                .font(.system(size: 64))
                                .foregroundStyle(.tint)
                                .frame(width: 140, height: 140)
                        } else {
                            Image("Otter")
                                .resizable()
                                .scaledToFill()
                                .frame(width: 140, height: 140)
                                .clipShape(Circle())
                                .overlay(Circle().stroke(.white, lineWidth: 3))
                                .shadow(radius: 4)
                                .accessibilityHidden(true)
                        }
                        Text(pages[index].title)
                            .font(.title.bold())
                            .multilineTextAlignment(.center)
                        Text(pages[index].text)
                            .font(.body)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                        Spacer()
                    }
                    .padding(.horizontal, 32)
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))

            VStack(spacing: 12) {
                Button {
                    if page < pages.count - 1 {
                        withAnimation { page += 1 }
                    } else {
                        dismiss()
                    }
                } label: {
                    Text(page < pages.count - 1 ? "Next" : "Get started")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)

                Button("Skip") { dismiss() }
                    .font(.subheadline)
                    .opacity(page < pages.count - 1 ? 1 : 0)
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 24)
        }
        .interactiveDismissDisabled(false)
    }
}

#Preview {
    TutorialView()
}
