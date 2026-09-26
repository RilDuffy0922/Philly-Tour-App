import SwiftUI
import UIKit

extension View {
    /// Lets the guided demo find this view on screen so it can be highlighted.
    func demoTarget(_ target: DemoTarget?) -> some View {
        modifier(DemoTargetModifier(target: target))
    }
}

private struct DemoTargetModifier: ViewModifier {
    let target: DemoTarget?
    @Environment(DemoController.self) private var demo

    func body(content: Content) -> some View {
        content.background {
            if let target {
                GeometryReader { proxy in
                    let frame = proxy.frame(in: .global)
                    Color.clear
                        .onAppear { report(frame, for: target) }
                        .onChange(of: frame) { _, newFrame in report(newFrame, for: target) }
                }
            }
        }
    }

    private func report(_ frame: CGRect, for target: DemoTarget) {
        guard demo.frames[target] != frame else { return }
        demo.frames[target] = frame
    }
}

/// Draws the demo on top of the app: a light blue glow around the feature being explained, the otter with a
/// speech bubble, and a "Tap to continue" prompt. `scope` says which screen this copy lives on, so the copy
/// inside the stop sheet shows only the sheet steps and the one at the root shows the rest.
struct DemoOverlay: View {
    enum Scope { case root, sheet }
    let scope: Scope

    @Environment(DemoController.self) private var demo
    @State private var pulse = false

    var body: some View {
        if demo.isActive, let step = demo.current, (step.screen == .sheet) == (scope == .sheet) {
            GeometryReader { proxy in
                let origin = proxy.frame(in: .global).origin
                let size = proxy.size
                let target = toolbarRect(for: step.target, width: size.width)
                    ?? demo.frames[step.target].map { $0.offsetBy(dx: -origin.x, dy: -origin.y) }
                // Keep the bubble clear of what it describes: on the tour screen, park it over the map for anything in the
                // bottom panel or the top bar; elsewhere put it at the bottom.
                let midY = target?.midY ?? 0
                let bubbleAtTop = midY > size.height * 0.6 || (step.screen == .tour && midY < size.height * 0.2)

                ZStack {
                    // Blocks the app underneath, and moves the demo along when the otter is done.
                    Color.black.opacity(0.001)
                        .contentShape(Rectangle())
                        .onTapGesture { demo.tap() }

                    if let target {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color.cyan.opacity(0.14))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.cyan, lineWidth: 3))
                            .shadow(color: .cyan.opacity(0.9), radius: pulse ? 18 : 8)
                            .frame(width: target.width + 12, height: target.height + 12)
                            .position(x: target.midX, y: target.midY)
                            .allowsHitTesting(false)
                    }

                    VStack {
                        if bubbleAtTop { bubble(step.text); Spacer() } else { Spacer(); bubble(step.text) }
                    }
                    .padding(.horizontal)
                    .padding(.top, scope == .sheet ? 56 : 120)
                    .padding(.bottom, bubbleAtTop ? 0 : (step.target == .map ? 262 : 76))
                    .allowsHitTesting(false)

                    if demo.awaitingTap {
                        VStack {
                            Spacer()
                            HStack {
                                Spacer()
                                Label("Tap to continue", systemImage: "hand.tap.fill")
                                    .font(.subheadline.weight(.semibold))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 9)
                                    .background(.regularMaterial, in: Capsule())
                                    .overlay(Capsule().stroke(Color.cyan, lineWidth: 2))
                                    .shadow(radius: 3)
                            }
                            .padding(.trailing, 16)
                            .padding(.bottom, scope == .sheet ? 16 : 6)
                        }
                        .allowsHitTesting(false)
                        .transition(.opacity)
                    }
                }
                .frame(width: size.width, height: size.height)
            }
            .ignoresSafeArea()
            .animation(.easeInOut(duration: 0.3), value: step.target)
            .animation(.easeInOut(duration: 0.25), value: demo.awaitingTap)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { pulse = true }
            }
        }
    }

    /// SwiftUI reports wrong positions for navigation bar buttons, so place them from the safe area instead:
    /// 44 pt buttons, 16 pt from the edge, centered in the bar.
    private func toolbarRect(for target: DemoTarget, width: CGFloat) -> CGRect? {
        let topInset = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.safeAreaInsets.top }.first ?? 59
        let side: CGFloat = 44
        let y = topInset + 22 - side / 2
        switch target {
        case .helpButton: return CGRect(x: 16, y: y, width: side, height: side)
        case .settingsButton, .menuButton: return CGRect(x: width - 16 - side, y: y, width: side, height: side)
        default: return nil
        }
    }

    private func bubble(_ text: String) -> some View {
        HStack(alignment: .bottom, spacing: 6) {
            Image("Otter")
                .resizable()
                .scaledToFill()
                .frame(width: 60, height: 60)
                .clipShape(Circle())
                .overlay(Circle().stroke(.white, lineWidth: 2))
                .shadow(radius: 2)
                .accessibilityHidden(true)

            Text(text)
                .font(.subheadline)
                .foregroundStyle(.primary)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(SpeechBubble().fill(.regularMaterial))
                .overlay(SpeechBubble().stroke(.secondary.opacity(0.25), lineWidth: 1))
                .accessibilityLabel("Otter says: \(text)")
        }
    }
}
